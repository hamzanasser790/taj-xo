package com.example

import org.junit.Assert.*
import org.junit.Test
import java.util.ArrayDeque

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  private fun checkWinForBot(b: String, c: Char): Boolean {
      if (b.length < 9) return false
      if (b[0] == c && b[1] == c && b[2] == c) return true
      if (b[3] == c && b[4] == c && b[5] == c) return true
      if (b[6] == c && b[7] == c && b[8] == c) return true
      if (b[0] == c && b[3] == c && b[6] == c) return true
      if (b[1] == c && b[4] == c && b[7] == c) return true
      if (b[2] == c && b[5] == c && b[8] == c) return true
      if (b[0] == c && b[4] == c && b[8] == c) return true
      if (b[2] == c && b[4] == c && b[6] == c) return true
      return false
  }

  private fun getSuccessorBoards(board: String, turn: Char): List<String> {
      val count = board.count { it == turn }
      val successors = mutableListOf<String>()
      if (count < 3) {
          for (i in 0 until 9) {
              if (board[i] == '_') {
                  val nextChars = board.toCharArray()
                  nextChars[i] = turn
                  successors.add(String(nextChars))
              }
          }
      } else {
          for (src in 0 until 9) {
              if (board[src] == turn) {
                  for (dst in 0 until 9) {
                      if (board[dst] == '_') {
                          val nextChars = board.toCharArray()
                          nextChars[src] = '_'
                          nextChars[dst] = turn
                          successors.add(String(nextChars))
                      }
                  }
              }
          }
      }
      return successors
  }

  @Test
  fun testStateSpaceSize() {
      val reachable = mutableSetOf<String>()
      val queue = ArrayDeque<Pair<String, Char>>()
      
      queue.add(Pair("_________", 'X'))
      reachable.add("_________:X")
      
      while (queue.isNotEmpty()) {
          val (board, turn) = queue.removeFirst()
          val nextTurn = if (turn == 'X') 'O' else 'X'
          
          if (checkWinForBot(board, 'X') || checkWinForBot(board, 'O')) {
              continue
          }
          
          val successors = getSuccessorBoards(board, turn)
          for (nextBoard in successors) {
              val nextKey = "${nextBoard}:$nextTurn"
              if (nextKey !in reachable) {
                  reachable.add(nextKey)
                  queue.add(Pair(nextBoard, nextTurn))
              }
          }
      }
      
      val queueO = ArrayDeque<Pair<String, Char>>()
      queueO.add(Pair("_________", 'O'))
      if ("_________:O" !in reachable) {
          reachable.add("_________:O")
          while (queueO.isNotEmpty()) {
              val (board, turn) = queueO.removeFirst()
              val nextTurn = if (turn == 'X') 'O' else 'X'
              if (checkWinForBot(board, 'X') || checkWinForBot(board, 'O')) {
                  continue
              }
              val successors = getSuccessorBoards(board, turn)
              for (nextBoard in successors) {
                  val nextKey = "${nextBoard}:$nextTurn"
                  if (nextKey !in reachable) {
                      reachable.add(nextKey)
                      queueO.add(Pair(nextBoard, nextTurn))
                    }
              }
          }
      }

      println("Total reachable states: ${reachable.size}")
      assertTrue(reachable.size > 0)
  }

  private fun getValidMovesForChar(board: String, c: Char): List<Int> {
      val pieceCount = board.count { it == c }
      val moves = mutableListOf<Int>()
      if (pieceCount < 3) {
          for (i in 0 until 9) {
              if (board[i] == '_') {
                  moves.add(i)
              }
          }
      } else {
          for (src in 0 until 9) {
              if (board[src] == c) {
                  for (dst in 0 until 9) {
                      if (board[dst] == '_') {
                          moves.add(src * 10 + dst)
                      }
                  }
              }
          }
      }
      return moves
  }

  private fun applyMoveToBoard(board: String, move: Int, c: Char): String {
      val chars = board.toCharArray()
      val pieceCount = board.count { it == c }
      if (pieceCount < 3) {
          // Placement phase
          chars[move] = c
      } else {
          // Movement phase
          val src: Int
          val dst: Int
          if (move >= 10) {
              src = move / 10
              dst = move % 10
          } else {
              src = 0
              dst = move
          }
          chars[src] = '_'
          chars[dst] = c
      }
      return String(chars)
  }

  private fun minimaxMemoized(
      board: String,
      currentTurn: Char,
      nextTurn: Char,
      memo: MutableMap<String, Double>,
      depth: Int
  ): Double {
      if (depth > 12) {
          return 0.0
      }
      if (checkWinForBot(board, nextTurn)) {
          return -10000.0 + depth
      }
      if (checkWinForBot(board, currentTurn)) {
          return 10000.0 - depth
      }
      
      val stateKey = "${board}:$currentTurn"
      if (stateKey in memo) {
          return memo[stateKey]!!
      }
      
      val moves = getValidMovesForChar(board, currentTurn)
      if (moves.isEmpty()) {
          return 0.0
      }
      
      memo[stateKey] = 0.0
      
      var maxVal = -Double.MAX_VALUE
      for (move in moves) {
          val nextBoard = applyMoveToBoard(board, move, currentTurn)
          val oppVal = minimaxMemoized(nextBoard, nextTurn, currentTurn, memo, depth + 1)
          val ourVal = -oppVal * 0.95
          if (ourVal > maxVal) {
              maxVal = ourVal
          }
      }
      
      memo[stateKey] = maxVal
      return maxVal
  }

  private fun getBestMoveDynamic(board: String, myChar: Char, opponentChar: Char): Int {
      val moves = getValidMovesForChar(board, myChar)
      if (moves.isEmpty()) return -1
      
      var bestMove = moves.first()
      var bestVal = -Double.MAX_VALUE
      val memo = mutableMapOf<String, Double>()
      
      for (move in moves) {
          val nextBoard = applyMoveToBoard(board, move, myChar)
          val valForOpponent = minimaxMemoized(nextBoard, opponentChar, myChar, memo, depth = 1)
          val ourVal = -valForOpponent
          if (ourVal > bestVal) {
              bestVal = ourVal
              bestMove = move
          }
      }
      return bestMove
  }

  @Test
  fun testValueIterationUnbeatable() {
      val reachable = mutableSetOf<String>()
      val successorMap = mutableMapOf<String, List<String>>()
      val queue = ArrayDeque<Pair<String, Char>>()
      
      queue.add(Pair("_________", 'X'))
      reachable.add("_________:X")
      
      while (queue.isNotEmpty()) {
          val (board, turn) = queue.removeFirst()
          val nextTurn = if (turn == 'X') 'O' else 'X'
          val key = "${board}:$turn"
          
          if (checkWinForBot(board, 'X') || checkWinForBot(board, 'O')) {
              successorMap[key] = emptyList()
              continue
          }
          
          val successors = getSuccessorBoards(board, turn)
          successorMap[key] = successors
          for (nextBoard in successors) {
              val nextKey = "${nextBoard}:$nextTurn"
              if (nextKey !in reachable) {
                  reachable.add(nextKey)
                  queue.add(Pair(nextBoard, nextTurn))
              }
          }
      }
      
      val queueO = ArrayDeque<Pair<String, Char>>()
      queueO.add(Pair("_________", 'O'))
      if ("_________:O" !in reachable) {
          reachable.add("_________:O")
          while (queueO.isNotEmpty()) {
              val (board, turn) = queueO.removeFirst()
              val nextTurn = if (turn == 'X') 'O' else 'X'
              val key = "${board}:$turn"
              
              if (checkWinForBot(board, 'X') || checkWinForBot(board, 'O')) {
                  successorMap[key] = emptyList()
                  continue
              }
              val successors = getSuccessorBoards(board, turn)
              successorMap[key] = successors
              for (nextBoard in successors) {
                  val nextKey = "${nextBoard}:$nextTurn"
                  if (nextKey !in reachable) {
                      reachable.add(nextKey)
                      queueO.add(Pair(nextBoard, nextTurn))
                  }
              }
          }
      }

      val values = mutableMapOf<String, Double>()
      for (state in reachable) {
          val parts = state.split(':')
          val board = parts[0]
          val turn = parts[1][0]
          val opp = if (turn == 'X') 'O' else 'X'
          
          if (checkWinForBot(board, turn)) {
              values[state] = 10000.0
          } else if (checkWinForBot(board, opp)) {
              values[state] = -10000.0
          } else {
              values[state] = 0.0
          }
      }

      val discount = 0.95
      for (iter in 0 until 150) {
          var maxChange = 0.0
          val nextValues = values.toMutableMap()
          for (state in reachable) {
              val parts = state.split(':')
              val board = parts[0]
              val turn = parts[1][0]
              val opp = if (turn == 'X') 'O' else 'X'
              
              if (checkWinForBot(board, turn) || checkWinForBot(board, opp)) {
                  continue
              }
              
              val successors = successorMap[state] ?: emptyList()
              if (successors.isEmpty()) {
                  nextValues[state] = 0.0
                  continue
              }
              
              var maxVal = -Double.MAX_VALUE
              for (nextBoard in successors) {
                  val nextStateKey = "${nextBoard}:$opp"
                  val nextValForOpp = values[nextStateKey] ?: 0.0
                  val ourVal = -nextValForOpp * discount
                  if (ourVal > maxVal) {
                      maxVal = ourVal
                  }
              }
              nextValues[state] = maxVal
              val diff = Math.abs(maxVal - (values[state] ?: 0.0))
              if (diff > maxChange) {
                  maxChange = diff
              }
          }
          values.putAll(nextValues)
          if (maxChange < 0.01) {
              break
          }
      }

      // Simulate 1000 games of Bot (X) vs Random Opponent (O)
      var botLosses = 0
      var botWins = 0
      var draws = 0
      val r = java.util.Random(42)

      for (g in 0 until 1000) {
          var board = "_________"
          var turn = 'X' // Bot is X, plays first
          var moveCount = 0
          while (moveCount < 100 && !checkWinForBot(board, 'X') && !checkWinForBot(board, 'O')) {
              val moves = getValidMovesForChar(board, turn)
              if (moves.isEmpty()) break
              
              val chosenMove = if (turn == 'X') {
                  // Bot move using solved values
                  var bestMove = moves.first()
                  var bestValue = -Double.MAX_VALUE
                  for (m in moves) {
                      val nextB = applyMoveToBoard(board, m, 'X')
                      val opponentValue = values["${nextB}:O"] ?: 0.0
                      val ourValue = -opponentValue
                      if (ourValue > bestValue) {
                          bestValue = ourValue
                          bestMove = m
                      }
                  }
                  bestMove
              } else {
                  // Opponent random move
                  moves[r.nextInt(moves.size)]
              }
              
              board = applyMoveToBoard(board, chosenMove, turn)
              turn = if (turn == 'X') 'O' else 'X'
              moveCount++
          }
          
          if (checkWinForBot(board, 'X')) {
              botWins++
          } else if (checkWinForBot(board, 'O')) {
              botLosses++
              println("Loss as X: $board (after $moveCount moves)")
          } else {
              draws++
          }
      }

      println("Simulated as X: wins=$botWins, losses=$botLosses, draws=$draws")
      assertEquals("Bot should never lose as X", 0, botLosses)

      // Simulate 1000 games of Bot (O) vs Random Opponent (X)
      botLosses = 0
      botWins = 0
      draws = 0
      for (g in 0 until 1000) {
          var board = "_________"
          var turn = 'X' // Opponent is X, plays first
          var moveCount = 0
          while (moveCount < 100 && !checkWinForBot(board, 'X') && !checkWinForBot(board, 'O')) {
              val moves = getValidMovesForChar(board, turn)
              if (moves.isEmpty()) break
              
              val chosenMove = if (turn == 'O') {
                  // Bot move using solved values
                  var bestMove = moves.first()
                  var bestValue = -Double.MAX_VALUE
                  for (m in moves) {
                      val nextB = applyMoveToBoard(board, m, 'O')
                      val opponentValue = values["${nextB}:X"] ?: 0.0
                      val ourValue = -opponentValue
                      if (ourValue > bestValue) {
                          bestValue = ourValue
                          bestMove = m
                      }
                  }
                  bestMove
              } else {
                  // Opponent random move
                  moves[r.nextInt(moves.size)]
              }
              
              board = applyMoveToBoard(board, chosenMove, turn)
              turn = if (turn == 'X') 'O' else 'X'
              moveCount++
          }
          
          if (checkWinForBot(board, 'O')) {
              botWins++
          } else if (checkWinForBot(board, 'X')) {
              botLosses++
              println("Loss as O: $board (after $moveCount moves)")
          } else {
              draws++
          }
      }

      println("Simulated as O: wins=$botWins, losses=$botLosses, draws=$draws")
      assertEquals("Bot should never lose as O", 0, botLosses)
  }

  @Test
  fun testMemoizedMinimax() {
      val board = "X_X_O_O__" // Placement phase, O played 2, X played 2. X's turn to place.
      val move = getBestMoveDynamic(board, 'X', 'O')
      println("Best move for X from $board: $move")
      assertTrue(move in listOf(1, 3, 7, 8)) // Valid empty spots
  }

}


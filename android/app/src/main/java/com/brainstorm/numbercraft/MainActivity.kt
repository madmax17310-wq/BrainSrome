package com.brainstorm.numbercraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

private val Ink = Color(0xFF271D42)
private val Muted = Color(0xFF716786)
private val Purple = Color(0xFF7651B8)
private val Violet = Color(0xFFF0E9FF)
private val Lime = Color(0xFFDDF5A8)
private val Gold = Color(0xFFFFE39B)
private val Paper = Color(0xFFFFFCF4)
private val White = Color(0xFFFFFFFF)

private data class MathPuzzle(val numbers: List<Int>, val target: Int)
private data class EquationToken(val type: String, val value: String, val index: Int = -1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NumbercraftApp() }
    }
}

@Composable
private fun NumbercraftApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("numbercraft_progress", 0) }

    var level by remember { mutableIntStateOf(prefs.getInt("level", 1).coerceAtLeast(1)) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var score by remember { mutableIntStateOf(prefs.getInt("score", 0)) }
    var stars by remember { mutableIntStateOf(prefs.getInt("stars", 0)) }
    var attempts by remember { mutableIntStateOf(prefs.getInt("attempts", 0)) }
    var solved by remember { mutableStateOf(prefs.getBoolean("solved", false)) }
    var feedback by remember { mutableStateOf("Tap the number tiles to start your equation!") }
    var puzzle by remember(level) { mutableStateOf(makePuzzle(level)) }
    val tokens = remember {
        mutableStateListOf<EquationToken>().apply {
            decodeTokens(prefs.getString("equation", "").orEmpty(), makePuzzle(level)).forEach { add(it) }
        }
    }

    fun save() {
        val encoded = tokens.joinToString("|") { token ->
            if (token.type == "n") "n:${token.index}" else "o:${token.value}"
        }
        prefs.edit()
            .putInt("level", level)
            .putInt("best", best)
            .putInt("score", score)
            .putInt("stars", stars)
            .putInt("attempts", attempts)
            .putBoolean("solved", solved)
            .putString("equation", encoded)
            .apply()
    }

    val usedNumberIndexes = tokens.filter { it.type == "n" }.map { it.index }.toSet()
    val wantsNumber = tokens.size % 2 == 0
    val equation = if (tokens.isEmpty()) "Your equation goes here" else tokens.joinToString(" ") { token ->
        when (token.type) {
            "n" -> puzzle.numbers.getOrNull(token.index)?.toString().orEmpty()
            "o" -> when (token.value) { "*" -> "\u00D7"; "/" -> "\u00F7"; else -> token.value }
            else -> ""
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, onPrimary = White, background = Paper, surface = White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(Lime, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("\uD83E\uDDE0", fontSize = 25.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NUMBERCRAFT", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Math Quest", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("\u2B50 $stars", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(22.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("LEVEL $level", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("You can do it!", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Gold), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TARGET", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(puzzle.target.toString(), color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Violet), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BUILD YOUR EQUATION", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "$equation   =   ${puzzle.target}",
                            modifier = Modifier.fillMaxWidth(),
                            color = if (tokens.isEmpty()) Muted else Ink,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                }
                Spacer(Modifier.height(19.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    puzzle.numbers.forEachIndexed { index, number ->
                        Button(
                            onClick = {
                                if (!solved && wantsNumber && tokens.size < 7 && index !in usedNumberIndexes) {
                                    tokens.add(EquationToken("n", number.toString(), index))
                                    feedback = "Nice! Now choose an operation."
                                    save()
                                }
                            },
                            enabled = !solved && wantsNumber && tokens.size < 7 && index !in usedNumberIndexes,
                            modifier = Modifier.weight(1f).heightIn(min = 72.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple, disabledContainerColor = Color(0xFFD8D2E5))
                        ) { Text(number.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black) }
                    }
                }

                Spacer(Modifier.height(11.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("+", "-", "*", "/").forEach { op ->
                        val label = when (op) { "*" -> "\u00D7"; "/" -> "\u00F7"; else -> op }
                        Button(
                            onClick = {
                                if (!solved && !wantsNumber && tokens.size < 7) {
                                    tokens.add(EquationToken("o", op))
                                    feedback = "Great! Choose another number."
                                    save()
                                }
                            },
                            enabled = !solved && !wantsNumber && tokens.size < 7,
                            modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4D6FF), contentColor = Ink, disabledContainerColor = Color(0xFFF0EDF5), disabledContentColor = Muted)
                        ) { Text(label, fontSize = 23.sp, fontWeight = FontWeight.Black) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton(text = "Undo", enabled = !solved && tokens.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        if (tokens.isNotEmpty()) tokens.removeAt(tokens.lastIndex)
                        feedback = "Keep going - try a different order."
                        save()
                    }
                    SmallButton(text = "Clear", enabled = !solved && tokens.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        tokens.clear(); feedback = "Start again with any number."; save()
                    }
                    SmallButton(text = "Hint", enabled = !solved, modifier = Modifier.weight(1f), color = Gold) {
                        feedback = "Hint: multiplication and division happen before adding and subtracting."
                    }
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val chosenNumbers = tokens.filter { it.type == "n" }.mapNotNull { puzzle.numbers.getOrNull(it.index) }
                        val operations = tokens.filter { it.type == "o" }.map { it.value }
                        attempts += 1
                        val answer = calculate(chosenNumbers, operations)
                        if (answer.isFinite() && abs(answer - puzzle.target) < 0.0000001) {
                            solved = true
                            val earned = when { attempts == 1 -> 3; attempts <= 3 -> 2; else -> 1 }
                            best = maxOf(best, level)
                            score += level * 100 + maxOf(0, 3 - attempts) * 25
                            stars += earned
                            feedback = "\uD83C\uDF89 Brilliant! You found it!"
                        } else {
                            feedback = if (answer.isFinite()) "That makes ${answer.prettyNumber()}. Try another way!" else "That operation cannot be used here. Try another way!"
                        }
                        save()
                    },
                    enabled = !solved && tokens.size == 7,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink, disabledContainerColor = Color(0xFFE8E4EC), disabledContentColor = Muted)
                ) { Text("Check my answer!", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }

                Spacer(Modifier.height(9.dp))
                Text(feedback, color = if (solved) Color(0xFF438344) else Muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp))

                if (solved) {
                    Spacer(Modifier.height(6.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2C9)), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\uD83C\uDFC6", fontSize = 36.sp)
                            Text("Level complete!", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("You earned ${if (attempts == 1) 3 else if (attempts <= 3) 2 else 1} stars!", color = Muted, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    level += 1; puzzle = makePuzzle(level); tokens.clear(); attempts = 0; solved = false
                                    feedback = "Level $level! Tap the number tiles to start your equation."
                                    save()
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp),
                                shape = RoundedCornerShape(17.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple)
                            ) { Text("Next puzzle  \u2192", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBadge(label = "BEST LEVEL", value = best.toString())
                    StatBadge(label = "SCORE", value = score.toString())
                    StatBadge(label = "STARS", value = stars.toString())
                }
                Spacer(Modifier.height(14.dp))
                Text("Made for curious minds \u2022 Progress stays on this device", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SmallButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, color: Color = Violet, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Ink, disabledContainerColor = Color(0xFFF0EDF5), disabledContentColor = Muted)
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

private fun makePuzzle(level: Int): MathPuzzle {
    if (level == 1) return MathPuzzle(listOf(2, 3, 5, 7), 24)
    val random = Random(level * 73 + 809)
    val maxNumber = (9 + level / 2).coerceAtMost(25)
    val allOperators = listOf("+", "-", "*", "/")
    repeat(5000) {
        val numbers = List(4) { random.nextInt(1, maxNumber + 1) }
        val operators = List(3) { allOperators.random(random) }
        if (operators.none { it == "*" || it == "/" }) return@repeat
        val result = calculate(numbers, operators)
        if (result.isFinite() && result % 1.0 == 0.0 && result > 1.0 && result <= 99999.0) {
            return MathPuzzle(numbers, result.toInt())
        }
    }
    val numbers = listOf(random.nextInt(1, maxNumber + 1), 2, 3, 4)
    return MathPuzzle(numbers, calculate(numbers, listOf("*", "+", "+")).toInt())
}

private fun calculate(numbers: List<Int>, operators: List<String>): Double {
    if (numbers.size != 4 || operators.size != 3) return Double.NaN
    val values = mutableListOf(numbers[0].toDouble())
    val lowOperators = mutableListOf<String>()
    for (index in operators.indices) {
        val operator = operators[index]
        val next = numbers[index + 1].toDouble()
        when (operator) {
            "*" -> values[values.lastIndex] = values.last() * next
            "/" -> {
                if (next == 0.0) return Double.NaN
                values[values.lastIndex] = values.last() / next
            }
            "+", "-" -> { lowOperators += operator; values += next }
            else -> return Double.NaN
        }
    }
    var answer = values.first()
    lowOperators.forEachIndexed { index, operator ->
        answer = if (operator == "+") answer + values[index + 1] else answer - values[index + 1]
    }
    return answer
}

private fun decodeTokens(encoded: String, puzzle: MathPuzzle): List<EquationToken> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split('|').mapNotNull { part ->
        val fields = part.split(':')
        when (fields.firstOrNull()) {
            "n" -> fields.getOrNull(1)?.toIntOrNull()?.takeIf { it in puzzle.numbers.indices }?.let { index ->
                EquationToken("n", puzzle.numbers[index].toString(), index)
            }
            "o" -> fields.getOrNull(1)?.takeIf { it in listOf("+", "-", "*", "/") }?.let { EquationToken("o", it) }
            else -> null
        }
    }.take(7)
}

private fun Double.prettyNumber(): String = if (abs(this - toInt()) < 0.0000001) toInt().toString() else "%.3f".format(this).trimEnd('0').trimEnd('.')


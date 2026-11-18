package com.brainstorm.numbercraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
private data class ChoicePuzzle(val prompt: String, val answer: String, val options: List<String>, val hint: String)
private data class GameMode(val id: String, val title: String, val icon: String, val description: String)

private val gameModes = listOf(
    GameMode("target", "Target Builder", "\uD83C\uDFAF", "Use four numbers to make the target."),
    GameMode("sequence", "Number Sequence", "\uD83D\uDD22", "Find the missing number in a pattern."),
    GameMode("times", "Times Tables", "\u2728", "Solve a multiplication mission."),
    GameMode("bonds", "Number Bonds", "\uD83E\uDDE9", "Find the number that completes the sum."),
    GameMode("fractions", "Fraction Finder", "\uD83C\uDF55", "Spot a fraction with the same value."),
    GameMode("shapes", "Shape Sequence", "\uD83D\uDD3A", "Choose the shape that comes next."),
    GameMode("odd", "Odd One Out", "\uD83D\uDD0E", "Find the number that breaks the pattern."),
    GameMode("place", "Place Value", "\uD83D\uDCA0", "Discover what a digit is really worth."),
    GameMode("balance", "Balance Beam", "\u2696\uFE0F", "Make both sides of the equation equal."),
    GameMode("riddle", "Number Riddle", "\uD83D\uDCA1", "Use the clues to solve a number mystery.")
)

private val starterPuzzles = listOf(
    MathPuzzle(listOf(2, 3, 5, 7), 24),
    MathPuzzle(listOf(2, 4, 5, 6), 27),
    MathPuzzle(listOf(3, 3, 6, 8), 42),
    MathPuzzle(listOf(4, 5, 7, 9), 64),
    MathPuzzle(listOf(2, 4, 6, 9), 56),
    MathPuzzle(listOf(2, 5, 8, 9), 79),
    MathPuzzle(listOf(4, 6, 7, 9), 65),
    MathPuzzle(listOf(2, 3, 8, 9), 67),
    MathPuzzle(listOf(3, 5, 7, 9), 61),
    MathPuzzle(listOf(4, 5, 8, 9), 81)
)

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
    var showMenu by remember { mutableStateOf(true) }
    var modeId by remember { mutableStateOf("target") }
    var level by remember { mutableIntStateOf(1) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var score by remember { mutableIntStateOf(prefs.getInt("score", 0)) }
    var stars by remember { mutableIntStateOf(prefs.getInt("stars", 0)) }
    var attempts by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var targetPuzzle by remember { mutableStateOf(makePuzzle(1)) }
    var choicePuzzle by remember { mutableStateOf(makeChoicePuzzle("sequence", 1)) }
    var menuRefresh by remember { mutableIntStateOf(0) }
    val tokens = remember { mutableStateListOf<EquationToken>() }
    val activeMode = gameModes.first { it.id == modeId }

    fun persist() {
        val prefix = "mode_${modeId}_"
        val equation = tokens.joinToString("|") { token ->
            if (token.type == "n") "n:${token.index}" else "o:${token.value}"
        }
        prefs.edit()
            .putInt("${prefix}level", level)
            .putInt("${prefix}attempts", attempts)
            .putBoolean("${prefix}solved", solved)
            .putString("${prefix}equation", equation)
            .putInt("best", best)
            .putInt("score", score)
            .putInt("stars", stars)
            .putInt("level", if (modeId == "target") level else prefs.getInt("level", 1))
            .putString("equation", if (modeId == "target") equation else prefs.getString("equation", ""))
            .apply()
        menuRefresh += 1
    }

    fun selectMode(id: String) {
        modeId = id
        val prefix = "mode_${id}_"
        level = prefs.getInt("${prefix}level", if (id == "target") prefs.getInt("level", 1) else 1).coerceAtLeast(1)
        attempts = prefs.getInt("${prefix}attempts", if (id == "target") prefs.getInt("attempts", 0) else 0)
        solved = prefs.getBoolean("${prefix}solved", if (id == "target") prefs.getBoolean("solved", false) else false)
        targetPuzzle = makePuzzle(level)
        choicePuzzle = makeChoicePuzzle(id, level)
        tokens.clear()
        val savedEquation = prefs.getString("${prefix}equation", if (id == "target") prefs.getString("equation", "") else "").orEmpty()
        decodeTokens(savedEquation, targetPuzzle).forEach { tokens.add(it) }
        feedback = when {
            solved -> "Wonderful work! You solved this puzzle."
            id == "target" -> "Choose your first tile to begin."
            else -> "Read the clue, then tap your answer."
        }
        showMenu = false
    }

    fun markSolved() {
        solved = true
        val earned = when { attempts <= 1 -> 3; attempts <= 3 -> 2; else -> 1 }
        best = maxOf(best, level)
        stars += earned
        score += level * 100 + maxOf(0, 3 - attempts) * 25
        feedback = "\uD83C\uDF89 Amazing! You found the answer!"
        persist()
    }

    fun checkTarget() {
        if (tokens.size != 7 || solved) return
        val numbers = tokens.filter { it.type == "n" }.mapNotNull { targetPuzzle.numbers.getOrNull(it.index) }
        val operations = tokens.filter { it.type == "o" }.map { it.value }
        attempts += 1
        val answer = calculate(numbers, operations)
        if (answer.isFinite() && abs(answer - targetPuzzle.target) < 0.0000001) markSolved()
        else feedback = if (answer.isFinite()) "That makes ${answer.prettyNumber()}. Try a new order!" else "That operation does not work here. Try again!"
        persist()
    }

    fun chooseAnswer(index: Int) {
        if (solved) return
        attempts += 1
        if (choicePuzzle.options.getOrNull(index) == choicePuzzle.answer) markSolved()
        else {
            feedback = "Good try! ${choicePuzzle.hint}"
            persist()
        }
    }

    fun nextPuzzle() {
        level += 1
        attempts = 0
        solved = false
        tokens.clear()
        targetPuzzle = makePuzzle(level)
        choicePuzzle = makeChoicePuzzle(modeId, level)
        feedback = "Level $level! You can do it."
        persist()
    }

    val modeLevels = remember(menuRefresh) {
        gameModes.associate { mode ->
            val prefix = "mode_${mode.id}_"
            mode.id to prefs.getInt("${prefix}level", if (mode.id == "target") prefs.getInt("level", 1) else 1)
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, onPrimary = White, background = Paper, surface = White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            if (showMenu) {
                GameMenu(levels = modeLevels, stars = stars, score = score, onSelect = ::selectMode)
            } else {
                GameScreen(
                    mode = activeMode,
                    level = level,
                    targetPuzzle = targetPuzzle,
                    choicePuzzle = choicePuzzle,
                    tokens = tokens,
                    solved = solved,
                    attempts = attempts,
                    best = best,
                    score = score,
                    stars = stars,
                    feedback = feedback,
                    onHome = { persist(); showMenu = true },
                    onNumber = { index ->
                        if (!solved && tokens.size < 7 && tokens.size % 2 == 0 && tokens.none { it.type == "n" && it.index == index }) {
                            tokens.add(EquationToken("n", targetPuzzle.numbers[index].toString(), index))
                            feedback = "Nice choice! Now pick an operation."
                            persist()
                        }
                    },
                    onOperator = { op ->
                        if (!solved && tokens.isNotEmpty() && tokens.size < 7 && tokens.size % 2 == 1) {
                            tokens.add(EquationToken("o", op))
                            feedback = "Great! Choose another number."
                            persist()
                        }
                    },
                    onUndo = {
                        if (tokens.isNotEmpty() && !solved) tokens.removeAt(tokens.lastIndex)
                        feedback = "Keep going - try a different order."
                        persist()
                    },
                    onClear = { if (!solved) { tokens.clear(); feedback = "Start again with any number."; persist() } },
                    onHint = {
                        feedback = if (modeId == "target") "Try multiplication or division before adding and subtracting." else choicePuzzle.hint
                    },
                    onCheck = ::checkTarget,
                    onChoice = ::chooseAnswer,
                    onNext = ::nextPuzzle
                )
            }
        }
    }
}

@Composable
private fun GameMenu(levels: Map<String, Int>, stars: Int, score: Int, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Lime, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                Text("\uD83E\uDDE0", fontSize = 28.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("NUMBERCRAFT", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("Math Quest", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("\u2B50 $stars", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Violet), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Pick a puzzle!", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Ten fun games for curious minds. Pick your favorite and play at your own pace.", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(7.dp))
                Text("SCORE  ${score.toString().padStart(4, '0')}     10 GAME TYPES", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        gameModes.chunked(2).forEach { rowModes ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowModes.forEach { mode ->
                    GameModeCard(mode = mode, level = levels[mode.id] ?: 1, modifier = Modifier.weight(1f), onClick = { onSelect(mode.id) })
                }
                if (rowModes.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        Text("Made for ages 7-12 \u2022 No ads \u2022 Plays offline", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GameModeCard(mode: GameMode, level: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 142.dp),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Ink)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
            Text(mode.icon, fontSize = 25.sp)
            Spacer(Modifier.height(4.dp))
            Text(mode.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 17.sp)
            Text(mode.description, color = Muted, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text("Level $level", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GameScreen(
    mode: GameMode,
    level: Int,
    targetPuzzle: MathPuzzle,
    choicePuzzle: ChoicePuzzle,
    tokens: List<EquationToken>,
    solved: Boolean,
    attempts: Int,
    best: Int,
    score: Int,
    stars: Int,
    feedback: String,
    onHome: () -> Unit,
    onNumber: (Int) -> Unit,
    onOperator: (String) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onHint: () -> Unit,
    onCheck: () -> Unit,
    onChoice: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onHome, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
                Text("\u2190 Games", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${mode.icon}  ${mode.title.uppercase()}", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                Text("Level $level", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("\u2B50 $stars", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))
        if (mode.id == "target") {
            TargetGame(
                puzzle = targetPuzzle, tokens = tokens, solved = solved,
                onNumber = onNumber, onOperator = onOperator, onUndo = onUndo,
                onClear = onClear, onHint = onHint, onCheck = onCheck
            )
        } else {
            ChoiceGame(puzzle = choicePuzzle, solved = solved, onChoice = onChoice, onHint = onHint)
        }

        Text(feedback, color = if (solved) Color(0xFF438344) else Muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).heightIn(min = 32.dp))

        if (solved) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2C9)), shape = RoundedCornerShape(21.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83C\uDFC6", fontSize = 35.sp)
                    Text("Puzzle complete!", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("You earned ${when { attempts <= 1 -> 3; attempts <= 3 -> 2; else -> 1 }} stars!", color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                        Text("Next puzzle  \u2192", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.height(11.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBadge(label = "BEST LEVEL", value = best.toString())
            StatBadge(label = "SCORE", value = score.toString())
            StatBadge(label = "STARS", value = stars.toString())
        }
        Spacer(Modifier.height(12.dp))
        Text("Progress stays on this device", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TargetGame(
    puzzle: MathPuzzle,
    tokens: List<EquationToken>,
    solved: Boolean,
    onNumber: (Int) -> Unit,
    onOperator: (String) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onHint: () -> Unit,
    onCheck: () -> Unit
) {
    val used = tokens.filter { it.type == "n" }.map { it.index }.toSet()
    val wantsNumber = tokens.size % 2 == 0
    val equation = if (tokens.isEmpty()) "Tap a number to begin" else tokens.joinToString(" ") { token ->
        if (token.type == "n") puzzle.numbers.getOrNull(token.index)?.toString().orEmpty()
        else when (token.value) { "*" -> "\u00D7"; "/" -> "\u00F7"; else -> token.value }
    }
    Text("Use each tile once. Make the target!", color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Gold), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TARGET NUMBER", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(puzzle.target.toString(), color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
    }
    Spacer(Modifier.height(12.dp))
    EquationCard(equation = equation, target = puzzle.target.toString())
    Spacer(Modifier.height(13.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        puzzle.numbers.forEachIndexed { index, number ->
            Button(
                onClick = { onNumber(index) },
                enabled = !solved && wantsNumber && tokens.size < 7 && index !in used,
                modifier = Modifier.weight(1f).heightIn(min = 66.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple, disabledContainerColor = Color(0xFFD8D2E5))
            ) { Text(number.toString(), fontSize = 25.sp, fontWeight = FontWeight.Black) }
        }
    }
    Spacer(Modifier.height(9.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("+", "-", "*", "/").forEach { op ->
            Button(
                onClick = { onOperator(op) },
                enabled = !solved && !wantsNumber && tokens.size < 7,
                modifier = Modifier.weight(1f).heightIn(min = 49.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink, disabledContainerColor = Color(0xFFF0EDF5), disabledContentColor = Muted)
            ) { Text(when (op) { "*" -> "\u00D7"; "/" -> "\u00F7"; else -> op }, fontSize = 21.sp, fontWeight = FontWeight.Black) }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        SmallButton("Undo", !solved && tokens.isNotEmpty(), Modifier.weight(1f), Violet, onUndo)
        SmallButton("Clear", !solved && tokens.isNotEmpty(), Modifier.weight(1f), Violet, onClear)
        SmallButton("Hint", !solved, Modifier.weight(1f), Gold, onHint)
        SmallButton("Check", !solved && tokens.size == 7, Modifier.weight(1.1f), Lime, onCheck)
    }
}

@Composable
private fun ChoiceGame(puzzle: ChoicePuzzle, solved: Boolean, onChoice: (Int) -> Unit, onHint: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Violet), shape = RoundedCornerShape(22.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PUZZLE", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(9.dp))
            Text(puzzle.prompt, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 30.sp)
        }
    }
    Spacer(Modifier.height(13.dp))
    puzzle.options.chunked(2).forEachIndexed { rowIndex, row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            row.forEachIndexed { colIndex, answer ->
                val index = rowIndex * 2 + colIndex
                Button(
                    onClick = { onChoice(index) },
                    enabled = !solved,
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Ink, disabledContainerColor = White, disabledContentColor = Ink)
                ) { Text(answer, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
    }
    SmallButton("\uD83D\uDCA1  Give me a hint", !solved, Modifier.fillMaxWidth(), Gold, onHint)
}

@Composable
private fun EquationCard(equation: String, target: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Violet), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("YOUR EQUATION", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(7.dp))
            Text("$equation   =   $target", color = if (equation.startsWith("Tap")) Muted else Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SmallButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, color: Color = Violet, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 47.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Ink, disabledContainerColor = Color(0xFFF0EDF5), disabledContentColor = Muted)) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

private fun makePuzzle(level: Int): MathPuzzle {
    starterPuzzles.getOrNull(level - 1)?.let { return it }
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

private fun makeChoicePuzzle(modeId: String, level: Int): ChoicePuzzle {
    val random = Random(modeId.hashCode() * 31 + level * 7919)
    fun numberQuestion(prompt: String, answer: Int, hint: String): ChoicePuzzle {
        val values = linkedSetOf(answer)
        var distance = 1
        while (values.size < 4) {
            val offset = if (distance % 2 == 1) (distance + 1) / 2 else -(distance / 2)
            values.add((answer + offset).coerceAtLeast(0))
            distance += 1
        }
        return ChoicePuzzle(prompt, answer.toString(), values.map(Int::toString).shuffled(random), hint)
    }
    return when (modeId) {
        "sequence" -> {
            val start = random.nextInt(1, 12)
            val step = 2 + (level - 1) % 6
            val row = (0..4).map { start + step * it }.joinToString(", ")
            numberQuestion("What number comes next?\n$row, ?", start + step * 5, "Look at how much the numbers grow each time.")
        }
        "times" -> {
            val maxFactor = (6 + level / 3).coerceAtMost(12)
            val a = random.nextInt(2, maxFactor + 1)
            val b = random.nextInt(2, maxFactor + 1)
            numberQuestion("What is $a \u00D7 $b?", a * b, "Try counting in groups of $a.")
        }
        "bonds" -> {
            val target = 15 + (level % 8) * 5
            val part = random.nextInt(3, target - 3)
            numberQuestion("Number bond: $part + ? = $target", target - part, "Think about how much is needed to reach $target.")
        }
        "fractions" -> {
            val denominator = random.nextInt(3, 10)
            val numerator = random.nextInt(1, denominator)
            val factor = 2 + level % 3
            val answer = "${numerator * factor}/${denominator * factor}"
            val options = listOf(answer, "${numerator + 1}/${denominator + 1}", "$numerator/$denominator", "${numerator * factor}/${denominator}").distinct().take(4).shuffled(random)
            val completed = if (options.size == 4) options else (options + listOf("1/2", "2/3", "3/4", "4/5").filterNot { it in options || it == answer }).take(4)
            ChoicePuzzle("Which fraction is equal to\n$numerator/$denominator?", answer, completed, "Equivalent fractions have the same value. Multiply the top and bottom by the same number.")
        }
        "shapes" -> {
            val sets = listOf(listOf("\u25CF", "\u25A0", "\u25B2"), listOf("\u2605", "\u25C6", "\u25CB"), listOf("\u25B2", "\u25A0", "\u25CF"))
            val shapes = sets[(level - 1) % sets.size]
            val row = listOf(shapes[0], shapes[1], shapes[2], shapes[0], shapes[1]).joinToString("   ")
            val options = (shapes + listOf("\u2605", "\u25A0", "\u2764")).distinct().take(4).shuffled(random)
            ChoicePuzzle("What shape comes next?\n$row   ?", shapes[2], options, "The shapes repeat in the same order.")
        }
        "odd" -> {
            val step = 2 + level % 4
            val start = random.nextInt(2, 15)
            val oddIndex = random.nextInt(1, 5)
            val row = (0..4).map { i -> start + step * i + if (i == oddIndex) 1 else 0 }
            val options = (listOf(row[oddIndex]) + row.filterIndexed { index, _ -> index != oddIndex }.take(3)).shuffled(random)
            ChoicePuzzle("One number breaks the count-by-$step pattern. Which one?\n${row.joinToString(", ")}", row[oddIndex].toString(), options.map(Int::toString), "All the other numbers go up by $step each time.")
        }
        "place" -> {
            val digits = List(4) { random.nextInt(1, 10) }
            val number = digits.joinToString("").toInt()
            val place = random.nextInt(0, 4)
            val digit = digits[3 - place]
            val answer = digit * when (place) { 0 -> 1; 1 -> 10; 2 -> 100; else -> 1000 }
            numberQuestion("In $number, what is the value of the ${when (place) { 0 -> "ones"; 1 -> "tens"; 2 -> "hundreds"; else -> "thousands" }} digit?", answer, "Look at the digit's position, not just the digit itself.")
        }
        "balance" -> {
            val leftA = random.nextInt(5, 20)
            val leftB = random.nextInt(4, 15)
            val rightA = random.nextInt(2, leftA + leftB - 1)
            numberQuestion("Balance the beam:\n$leftA + $leftB = $rightA + ?", leftA + leftB - rightA, "Find the total on the left, then fill the missing part on the right.")
        }
        "riddle" -> {
            val tens = random.nextInt(2, 6)
            val ones = random.nextListOfEvenDigit()
            val sum = tens + ones
            val answer = tens * 10 + ones
            numberQuestion("Number riddle: I am even. My tens digit is $tens, and my digits add to $sum. What am I?", answer, "Use the tens digit clue first, then find the ones digit.")
        }
        else -> ChoicePuzzle("Pick a number game!", "24", listOf("24", "18", "12", "30"), "Try the target puzzle.")
    }
}

private fun Random.nextListOfEvenDigit(): Int = listOf(0, 2, 4, 6, 8)[nextInt(0, 5)]

private fun calculate(numbers: List<Int>, operators: List<String>): Double {
    if (numbers.size != 4 || operators.size != 3) return Double.NaN
    val values = mutableListOf(numbers[0].toDouble())
    val lowOperators = mutableListOf<String>()
    for (index in operators.indices) {
        val operator = operators[index]
        val next = numbers[index + 1].toDouble()
        when (operator) {
            "*" -> values[values.lastIndex] = values.last() * next
            "/" -> { if (next == 0.0) return Double.NaN; values[values.lastIndex] = values.last() / next }
            "+", "-" -> { lowOperators += operator; values += next }
            else -> return Double.NaN
        }
    }
    var answer = values.first()
    lowOperators.forEachIndexed { index, operator -> answer = if (operator == "+") answer + values[index + 1] else answer - values[index + 1] }
    return answer
}

private fun decodeTokens(encoded: String, puzzle: MathPuzzle): List<EquationToken> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split('|').mapNotNull { part ->
        val fields = part.split(':')
        when (fields.firstOrNull()) {
            "n" -> fields.getOrNull(1)?.toIntOrNull()?.takeIf { it in puzzle.numbers.indices }?.let { index -> EquationToken("n", puzzle.numbers[index].toString(), index) }
            "o" -> fields.getOrNull(1)?.takeIf { it in listOf("+", "-", "*", "/") }?.let { EquationToken("o", it) }
            else -> null
        }
    }.take(7)
}

private fun Double.prettyNumber(): String = if (abs(this - toInt()) < 0.0000001) toInt().toString() else "%.3f".format(this).trimEnd('0').trimEnd('.')


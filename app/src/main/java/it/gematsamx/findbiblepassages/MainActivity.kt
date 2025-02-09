package it.gematsamx.findbiblepassages

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import java.text.Normalizer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindBibleBooksTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FindBibleBooksApp()
                }
            }
        }
    }
}

@Composable
fun FindBibleBooksApp() {
    var textState by remember { mutableStateOf(TextFieldValue()) }
    var outputText by remember { mutableStateOf("Digita il libro biblico ed eventualmente il capitolo e il versetto nel campo qua sopra per trovarlo più velocemente. Verrai reindirizzato automaticamente.") }
    val context = LocalContext.current

    val bibleBooks = listOf(
        "Genesi", "Esodo", "Levitico", "Numeri", "Deuteronomio",
        "Giosuè", "Giudici", "Rut",
        "1 Samuele", "2 Samuele",
        "1 Re", "2 Re",
        "1 Cronache", "2 Cronache",
        "Esdra", "Neemia", "Ester",
        "Giobbe", "Salmi", "Proverbi", "Ecclesiaste", "Cantico dei Cantici",
        "Isaia", "Geremia", "Lamentazioni", "Ezechiele", "Daniele",
        "Osea", "Gioele", "Amos", "Abdia", "Giona", "Michea",
        "Naum", "Abacuc", "Sofonia", "Aggeo", "Zaccaria", "Malachia",
        "Matteo", "Marco", "Luca", "Giovanni",
        "Atti",
        "Romani", "1 Corinti", "2 Corinti", "Galati", "Efesini",
        "Filippesi", "Colossesi",
        "1 Tessalonicesi", "2 Tessalonicesi",
        "1 Timoteo", "2 Timoteo",
        "Tito", "Filemone",
        "Ebrei",
        "Giacomo",
        "1 Pietro", "2 Pietro",
        "1 Giovanni", "2 Giovanni", "3 Giovanni",
        "Giuda", "Rivelazione"
    )

    fun getBookNumber(bookName: String): String? {
        val index = bibleBooks.indexOf(bookName)
        return if (index != -1) {
            (index + 1).toString().padStart(2, '0')
        } else {
            null
        }
    }

    fun checkBibleBook(input: String) {
        fun openLink(link: String) {
            val packageName = "org.jw.jwlibrary.mobile"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "JW Library non è installata sul dispositivo. Apro il link nel browser.", Toast.LENGTH_SHORT).show()
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        }

        val regex = Regex("""^(\d*\s?\p{L}(?:[\p{L}\s]+)?)\s*(\d+)?[\s:.]?(\d+)?""")
        val matchResult = regex.find(input.trim())

        if (matchResult != null) {
            val bookInput = matchResult.groupValues[1]
            val chapterInput = matchResult.groupValues.getOrNull(2)
            val verseInput = matchResult.groupValues.getOrNull(3)

            val normalizedBookInput = normalizeInput(bookInput)
            val matchingBooks = bibleBooks.filter {
                normalizeInput(it).startsWith(normalizedBookInput)
            }

            when {
                matchingBooks.size == 1 -> {
                    val bookName = matchingBooks[0]
                    val bookNumber = getBookNumber(bookName)

                    if (bookNumber != null) {
                        val chapterNumber = chapterInput?.toIntOrNull()
                        val verseNumber = verseInput?.toIntOrNull()

                        val link = when {
                            chapterInput.isNullOrBlank() -> "https://www.jw.org/finder?wtlocale=I&prefer=lang&book=$bookNumber&pub=nwtsty"
                            chapterNumber == null || chapterNumber <= 0 -> {
                                outputText = "Il capitolo deve essere un numero valido maggiore di 0."
                                return
                            }
                            else -> {
                                val formattedChapter = formatChapterNumber(chapterNumber)
                                val formattedVerse = verseNumber?.let { formatVerseNumber(it) } ?: ""

                                if (formattedVerse.isNotEmpty()) {
                                    "https://www.jw.org/finder?wtlocale=I&prefer=lang&bible=${bookNumber}${formattedChapter}${formattedVerse}&pub=nwtsty"
                                } else {
                                    "https://www.jw.org/finder?wtlocale=I&prefer=lang&bible=${bookNumber}${formattedChapter}000-${bookNumber}${formattedChapter}999&pub=nwtsty"
                                }
                            }
                        }
                        openLink(link)
                    }
                }
                matchingBooks.isNotEmpty() -> outputText = "Il testo non è sufficiente, sii più specifico. Si intendeva forse: ${matchingBooks.joinToString(" - ")}"
                else -> outputText = "Il passo biblico non è stato trovato. Controlla di averlo scritto correttamente!"
            }
        } else {
            outputText = "Formato non riconosciuto. Usa ad esempio: 'gen 3:5', 'eso 4 5', 'mat 2.4'."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "FindBiblePassages - Trova Passi Biblici", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(20.dp))
        BasicTextField(
            value = textState,
            onValueChange = {
                textState = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(10.dp)
                .border(1.dp, MaterialTheme.colorScheme.secondary),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 30.sp,
                color= MaterialTheme.colorScheme.primary
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                checkBibleBook(textState.text)
            },
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary, // Colore di sfondo del bottone
                contentColor = MaterialTheme.colorScheme.primary, // Colore del testo
        )
        ) {
            Text("Cerca")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = outputText, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FindBibleBooksTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xfff4f4f9),
            secondary = Color(0xff704CC7),
            tertiary = Color(0xff704CC7),
            background = Color(0xff1c1c1c),
        )
    } else {
        lightColorScheme(
            primary = Color(0xff1c1c1c),
            secondary = Color(0xFF6604D3),
            tertiary = Color(0xFFA374EE),
            background = Color(0xfff4f4f9)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

fun normalizeInput(input: String): String {
    val normalized = removeAccents(input)
        .replace(Regex("\\s+"), "")
        .lowercase()
        .trim()
    return normalized.replaceNames().replaceWrittenNumbers()
}

fun String.replaceWrittenNumbers(): String {
    val numericMap = mapOf(
        "primo" to "1", "prima" to "1",
        "secondo" to "2", "seconda" to "2",
        "terzo" to "3", "terza" to "3"
    )

    var result = this
    numericMap.forEach { (text, number) ->
        result = result.replace(text, number, ignoreCase = true)
    }
    return result
}

fun String.replaceNames(): String {
    val namesToReplace = mapOf(
        "salmo" to "salmi",
        "apocalisse" to "rivelazione",
        "qoelet" to "ecclesiaste",
        "librodeire" to "re",
        "corinzi" to "corinti"
    )

    var result = this
    namesToReplace.forEach { (text, name) ->
        result = result.replace(text, name, ignoreCase = true)
    }
    return result
}

fun removeAccents(input: String): String {
    return Normalizer.normalize(input, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}

fun formatChapterNumber(chapter: Int): String {
    return chapter.toString().padStart(3, '0')
}

fun formatVerseNumber(verse: Int): String {
    return verse.toString().padStart(3, '0')
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FindBibleBooksTheme {
        FindBibleBooksApp()
    }
}

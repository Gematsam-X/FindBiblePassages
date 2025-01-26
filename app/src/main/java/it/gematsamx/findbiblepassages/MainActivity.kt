package it.gematsamx.findbiblepassages

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import java.text.Normalizer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindBibleBooksApp()
        }
    }
}

@Composable
fun FindBibleBooksApp() {
    var textState by remember { mutableStateOf(TextFieldValue()) }
    var outputText by remember { mutableStateOf("Digita il libro biblico ed eventualmente il capitolo e il versetto nel campo qua sopra per trovarlo più velocemente. Verrai reindirizzato automaticamente.") }
    val context = LocalContext.current // Prendiamo il contesto

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

    // Funzione che restituisce il numero di un libro nella lista
    fun getBookNumber(bookName: String): String? {
        val index = bibleBooks.indexOf(bookName)
        return if (index != -1) {
            // La numerazione parte da 01 (Genesi è 01, Esodo è 02, ...)
            val bookNumber = (index + 1).toString().padStart(2, '0')
            bookNumber
        } else {
            null
        }
    }

    // Funzione che verifica il libro biblico e costruisce il link dinamico
    fun checkBibleBook(input: String) {
        fun openLink(link: String) {
            val packageName = "org.jw.jwlibrary.mobile"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                // Forza il targeting di JW Library
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback: se JW Library non è installata, apri il link in un browser
                Toast.makeText(context, "JW Library non è installata sul dispositivo. Apro il link nel browser.", Toast.LENGTH_SHORT).show()
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        }


        // Regex per catturare: "gen 3 5", "gen 3:5", "gen 3.5"
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

                        if (chapterInput.isNullOrBlank()) {
                            val link = "https://www.jw.org/finder?wtlocale=I&prefer=lang&book=$bookNumber&pub=nwtsty"
                            openLink(link)
                        } else if (chapterNumber == null || chapterNumber <= 0) {
                            outputText = "Il capitolo deve essere un numero valido maggiore di 0."
                        } else {
                            val formattedChapter = formatChapterNumber(chapterNumber)
                            val formattedVerse = verseNumber?.let { formatVerseNumber(it) } ?: ""

                            val link = if (formattedVerse.isNotEmpty()) {
                                "https://www.jw.org/finder?wtlocale=I&prefer=lang&bible=${bookNumber}${formattedChapter}${formattedVerse}&pub=nwtsty"
                            } else {
                                "https://www.jw.org/finder?wtlocale=I&prefer=lang&bible=${bookNumber}${formattedChapter}000-${bookNumber}${formattedChapter}999&pub=nwtsty"
                            }
                            openLink(link)
                        }
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
                textState = it // Aggiorniamo solo lo stato del campo di testo, senza fare la ricerca
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(10.dp)
                .border(1.dp, MaterialTheme.colorScheme.primary),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 30.sp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                // Si attiva la ricerca quando viene cliccato il tasto "Cerca"
                checkBibleBook(textState.text)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Cerca")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = outputText, style = MaterialTheme.typography.bodyLarge)
    }
}

fun removeAccents(input: String): String {
    return Normalizer.normalize(input, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}

// Funzione di normalizzazione dell'input
fun normalizeInput(input: String): String {
    val synonymMap = mapOf(
        "salmo" to "salmi",
        "apocalisse" to "rivelazione",
        "qoelet" to "ecclesiaste",
    )
    val normalized = removeAccents(input)
        .replace(Regex("\\s+"), "") // Rimuove spazi
        .lowercase() // Converte tutto in minuscolo
        .trim()
    val updated = synonymMap.getOrDefault(normalized, normalized)
    return updated
}

// Funzione per formattare il capitolo
fun formatChapterNumber(chapter: Int): String {
    return when {
        chapter < 10 -> "00$chapter"
        chapter < 100 -> "0$chapter"
        else -> chapter.toString()
    }
}

// Funzione per formattare il versetto
fun formatVerseNumber(verse: Int): String {
    return when {
        verse < 10 -> "00$verse"
        verse < 100 -> "0$verse"
        else -> verse.toString()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FindBibleBooksApp()
}
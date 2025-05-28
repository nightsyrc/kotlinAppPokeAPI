import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

// Configure JSON parser to ignore unknown keys
val jsonParser = Json {
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Pokemon App - Mewtwo"
    ) {
        MaterialTheme {
            var clickCount by remember { mutableStateOf(0) }
            var pokemonData by remember { mutableStateOf<Pokemon?>(null) }
            var isLoading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                isLoading = true
                try {
                    pokemonData = fetchPokemonData("mewtwo")
                    error = null
                } catch (e: Exception) {
                    error = e.message ?: "Failed to load Pokemon data"
                } finally {
                    isLoading = false
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Mewtwo Data from PokeAPI") },
                        actions = {
                            IconButton(onClick = { /* Action */ }) {
                                Icon(
                                    imageVector = Icons.Default.VideogameAsset,
                                    contentDescription = "Pokemon"
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Button Counter Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Button clicked $clickCount times",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Button(
                            onClick = { clickCount++ },
                            modifier = Modifier.widthIn(min = 200.dp)
                        ) {
                            Text("Click Me!")
                        }
                    }

                    // Pokemon Data Section
                    when {
                        isLoading -> CircularProgressIndicator()
                        error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        pokemonData != null -> {
                            val pokemon = pokemonData!!
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    pokemon.name.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text("Height: ${pokemon.height / 10.0} m")
                                Text("Weight: ${pokemon.weight / 10.0} kg")

                                pokemon.sprites.frontDefault?.let { imageUrl ->
                                    AsyncImage(
                                        imageUrl = imageUrl,
                                        contentDescription = pokemon.name,
                                        modifier = Modifier.size(200.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AsyncImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUrl) {
        try {
            val loadedImage = withContext(Dispatchers.IO) {
                val inputStream = URL(imageUrl).openStream()
                val bufferedImage = ImageIO.read(inputStream)
                bufferedImage.toComposeImageBitmap()
            }
            image = loadedImage
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Failed to load image"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            isLoading -> CircularProgressIndicator()
            error != null -> Text("Image load failed", color = MaterialTheme.colorScheme.error)
            image != null -> Image(
                bitmap = image!!,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }
    }
}

fun BufferedImage.toComposeImageBitmap(): ImageBitmap {
    val width = this.width
    val height = this.height
    val imageBitmap = ImageBitmap(width, height)
    val canvas = Canvas(imageBitmap)
    val paint = Paint()

    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = Color(this.getRGB(x, y))
            paint.color = color
            canvas.drawRect(
                Rect(
                    left = x.toFloat(),
                    top = y.toFloat(),
                    right = (x + 1).toFloat(),
                    bottom = (y + 1).toFloat()
                ),
                paint
            )
        }
    }

    return imageBitmap
}

@Serializable
data class Pokemon(
    val name: String,
    val height: Int,
    val weight: Int,
    val sprites: Sprites
)

@Serializable
data class Sprites(
    val front_default: String? = null
) {
    val frontDefault: String? get() = front_default
}

suspend fun fetchPokemonData(pokemonName: String): Pokemon {
    return withContext(Dispatchers.IO) {
        val url = URL("https://pokeapi.co/api/v2/pokemon/$pokemonName")
        url.openStream().use { stream ->
            jsonParser.decodeFromString<Pokemon>(stream.bufferedReader().readText())
        }
    }
}
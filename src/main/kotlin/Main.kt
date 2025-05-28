// Import necessary libraries and components
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

// Configure JSON parser to ignore extra fields we don't need from the API
val jsonParser = Json {
    ignoreUnknownKeys = true  // Skip any unexpected data from the API
}

// The main function that launches our desktop application
@OptIn(ExperimentalMaterial3Api::class)  // Allow using new Material 3 features
fun main() = application {  // Create a desktop window
    Window(
        onCloseRequest = ::exitApplication,  // Close app when window is closed
        title = "Pokemon App - Mewtwo"  // Window title
    ) {
        // Use Material Design styling
        MaterialTheme {
            // State variables to track:
            var clickCount by remember { mutableStateOf(0) }  // Button clicks
            var pokemonData by remember { mutableStateOf<Pokemon?>(null) }  // Pokemon data
            var isLoading by remember { mutableStateOf(false) }  // Loading status
            var error by remember { mutableStateOf<String?>(null) }  // Error messages

            // Load Pokemon data when app starts
            LaunchedEffect(Unit) {
                isLoading = true  // Show loading spinner
                try {
                    // Fetch Mewtwo's data from API
                    pokemonData = fetchPokemonData("mewtwo")
                    error = null  // Clear any previous errors
                } catch (e: Exception) {
                    // Show error if something goes wrong
                    error = e.message ?: "Failed to load Pokemon data"
                } finally {
                    isLoading = false  // Hide loading spinner
                }
            }

            // Main app layout
            Scaffold(
                topBar = {
                    // App bar at top of screen
                    TopAppBar(
                        title = { Text("Mewtwo Data from PokeAPI") },
                        actions = {
                            // Game controller icon button
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
                // Main content column
                Column(
                    Modifier
                        .fillMaxSize()  // Take up all available space
                        .padding(padding)  // Add spacing from app bar
                        .padding(16.dp),  // Add padding around edges
                    verticalArrangement = Arrangement.spacedBy(16.dp),  // Space between items
                    horizontalAlignment = Alignment.CenterHorizontally  // Center everything
                ) {
                    // Button Counter Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Show click count
                        Text(
                            "Button clicked $clickCount times",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        // Clickable button
                        Button(
                            onClick = { clickCount++ },  // Increase count when clicked
                            modifier = Modifier.widthIn(min = 200.dp)  // Minimum width
                        ) {
                            Text("Click Me!")
                        }
                    }

                    // Pokemon Data Section - shows different views based on state
                    when {
                        isLoading -> CircularProgressIndicator()  // Loading spinner
                        error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        pokemonData != null -> {  // Show Pokemon data when loaded
                            val pokemon = pokemonData!!
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Pokemon name (capitalized)
                                Text(
                                    pokemon.name.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                // Height (converted from decimeters to meters)
                                Text("Height: ${pokemon.height / 10.0} m")
                                // Weight (converted from hectograms to kilograms)
                                Text("Weight: ${pokemon.weight / 10.0} kg")

                                // Load and show Pokemon image if URL exists
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

// Custom component to load and display images from URLs
@Composable
fun AsyncImage(
    imageUrl: String,  // URL of image to load
    contentDescription: String,  // Accessibility description
    modifier: Modifier = Modifier  // Size/positioning modifiers
) {
    // State variables:
    var image by remember { mutableStateOf<ImageBitmap?>(null) }  // Loaded image
    var isLoading by remember { mutableStateOf(true) }  // Loading status
    var error by remember { mutableStateOf<String?>(null) }  // Error message

    // Load image when URL changes
    LaunchedEffect(imageUrl) {
        try {
            // Load in background thread to avoid freezing UI
            val loadedImage = withContext(Dispatchers.IO) {
                val inputStream = URL(imageUrl).openStream()  // Open connection
                val bufferedImage = ImageIO.read(inputStream)  // Read image data
                bufferedImage.toComposeImageBitmap()  // Convert to Compose format
            }
            image = loadedImage  // Store loaded image
            error = null  // Clear errors
        } catch (e: Exception) {
            error = e.message ?: "Failed to load image"  // Show error
        } finally {
            isLoading = false  // Loading complete
        }
    }

    // Show different views based on state
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            isLoading -> CircularProgressIndicator()  // Loading spinner
            error != null -> Text("Image load failed", color = MaterialTheme.colorScheme.error)
            image != null -> Image(  // Show actual image when loaded
                bitmap = image!!,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }
    }
}

// Convert Java's BufferedImage to Compose's ImageBitmap
fun BufferedImage.toComposeImageBitmap(): ImageBitmap {
    val width = this.width
    val height = this.height
    val imageBitmap = ImageBitmap(width, height)  // Create blank image
    val canvas = Canvas(imageBitmap)  // Get drawing canvas
    val paint = Paint()  // Create paint brush

    // Copy each pixel from source to destination
    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = Color(this.getRGB(x, y))  // Get pixel color
            paint.color = color  // Set brush color
            // Draw a 1x1 pixel rectangle
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

    return imageBitmap  // Return the new image
}

// Data class for Pokemon information from API
@Serializable  // Mark as serializable for JSON parsing
data class Pokemon(
    val name: String,  // Pokemon name
    val height: Int,  // Height in decimeters (10cm units)
    val weight: Int,  // Weight in hectograms (100g units)
    val sprites: Sprites  // Image URLs
)

// Data class for Pokemon images
@Serializable
data class Sprites(
    val front_default: String? = null  // Default front image URL
) {
    // Better-named property for the same value
    val frontDefault: String? get() = front_default
}

// Fetch Pokemon data from PokeAPI
suspend fun fetchPokemonData(pokemonName: String): Pokemon {
    // Run in background thread to avoid freezing UI
    return withContext(Dispatchers.IO) {
        val url = URL("https://pokeapi.co/api/v2/pokemon/$pokemonName")
        url.openStream().use { stream ->  // Automatically close connection
            // Parse JSON response into Pokemon object
            jsonParser.decodeFromString<Pokemon>(stream.bufferedReader().readText())
        }
    }
}
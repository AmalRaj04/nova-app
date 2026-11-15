import kotlin.test.Test
import kotlin.test.assertNotNull

class ResourcesTest {
    @Test
    fun mapFilesAreOnTheTestClasspath() {
        assertNotNull(javaClass.classLoader.getResource("map.json"))
        assertNotNull(javaClass.classLoader.getResource("markers.json"))
    }
}

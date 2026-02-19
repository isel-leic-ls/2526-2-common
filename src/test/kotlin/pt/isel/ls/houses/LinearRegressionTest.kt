package pt.isel.ls.houses

import kotlin.math.absoluteValue
import kotlin.test.*

class LinearRegressionTest {

    // ===============================
    // 1. Testes à normalização
    // ===============================

    @Test
    fun `scale should normalize min to 0 and max to 1`() {
        val scale = Scale(listOf(10.0, 20.0, 30.0))
        assertEquals(0.0, scale.normalize(10.0), 1e-9)
        assertEquals(1.0, scale.normalize(30.0), 1e-9)
    }

    @Test
    fun `denormalize should invert normalize`() {
        val scale = Scale(listOf(5.0, 15.0))
        val value = 10.0
        val normalized = scale.normalize(value)
        assertEquals(0.5, normalized, 1e-9)
        val denormalized = scale.denormalize(normalized)
        assertEquals(value, denormalized, 1e-9)
    }

    @Test
    fun `normalize list of houses should produce values between 0 and 1`() {
        val normalized = houses.normalize()
        normalized.data.forEach {
            assertTrue(it.area in 0.0..1.0)
            assertTrue(it.price in 0.0..1.0)
        }
    }

    // ===============================
    // 2. Testes às funções auxiliares
    // ===============================

    @Test
    fun `predict should compute linear function`() {
        val params = Params(w = 2.0, b = 3.0)
        val result = predict(4.0, params)
        assertEquals(11.0, result, 1e-9)
    }

    @Test
    fun `error should compute difference between prediction and real`() {
        val err = error(10.0, 8.0)
        assertEquals(2.0, err, 1e-9)
    }

    @Test
    fun `gradients should compute correct partial derivatives`() {
        val grad = gradients(
            x = 2.0,
            error = 3.0,
            n = 4
        )
        assertEquals((2.0 / 4) * 3.0 * 2.0, grad.w, 1e-9)
        assertEquals((2.0 / 4) * 3.0, grad.b, 1e-9)
    }

    @Test
    fun `updateParams should apply gradient descent step`() {
        val p = Params(1.0, 1.0)
        val delta = Params(0.5, 0.5)
        val lr = 0.1
        val (w,b) = updateParams(p, delta, lr)
        assertEquals(0.95, w, 1e-9)
        assertEquals(0.95, b, 1e-9)
    }

    // ===============================
    // 3. Testes ao treino
    // ===============================

    @Test
    fun `train should reduce error on simple linear data`() {
        // y = 2x
        val simpleData = listOf(
            House(0.0, 0.0),
            House(0.5, 1.0),
            House(1.0, 2.0)
        )
        val params = train(simpleData, epochs = 5000, lr = 0.1)
        // Esperamos w próximo de 2 e b próximo de 0
        assertTrue((params.w - 2.0).absoluteValue < 0.1)
        assertTrue((params.b).absoluteValue < 0.1)
    }

    @Test
    fun `trained model should predict reasonably close to real data`() {
        val (_, _, data) = houses.normalize()
        val params = train(data, epochs = 4000, lr = 0.05)
        val house = data.first()
        val prediction = predict(house.area, params)
        val err = (prediction - house.price).absoluteValue
        println(err)
        assertTrue(err < 0.05) // erro pequeno na escala normalizada
    }
}
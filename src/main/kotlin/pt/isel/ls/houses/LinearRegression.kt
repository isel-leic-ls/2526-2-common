package pt.isel.ls.houses

import kotlin.math.roundToLong

/*
 Regressão Linear em Kotlin
 Previsão de preço de casas sabendo a àrea em m²
*/

// ===============================
// 1. Estrutura de dados
// ===============================

// Representa uma casa com área (m²) e preço (euros)
data class House(val area: Double, val price: Double)

// Dados originais com intervalos vazios reais
val houses = listOf(
    House(35.0, 120000.0),
    House(52.0, 155000.0),
    House(70.0, 210000.0),
    House(95.0, 260000.0),
    // gap: não temos dados entre 95 e 140
    House(140.0, 340000.0),
    // gap grande
    House(220.0, 480000.0)
)

// ===============================
// 2. Normalização
// ===============================

// Escala de valores a normalizar
class Scale(values: List<Double>) {
    val min: Double = values.min()  // Valor mínimo da escala
    val max: Double = values.max()  // Valor máximo da escala
    val delta: Double = max - min   // Comprimento da escala
    // val delta: Double = max + min   // Comprimento da escala [ERROR]
    // Normaliza um valor para o intervalo [0, 1]
    fun normalize(value: Double) = (value - min) / delta
    // Desnormaliza um valor para escala original
    fun denormalize(value: Double) = value * delta + min
}

// Representa dados normalizados + escalas de normalização
data class NormalizedData(
    val areas: Scale,   // Escala das áreas
    val prices: Scale,  // Escala dos preços
    val data: List<House> // Dados normalizados
)

// Normaliza dados
fun List<House>.normalize(): NormalizedData {
    val areas = Scale(map { it.area })
    val prices = Scale(map { it.price })
    return NormalizedData(areas, prices, data = map {
        House(areas.normalize(it.area), prices.normalize(it.price))
    })
}

// ===============================
// 3. Modelo
// ===============================

// Parâmetros do modelo: weight (w) e bias (b)
data class Params(val w: Double, val b: Double)

operator fun Params.plus(other: Params) = Params(w + other.w, b + other.b)

// Função hipótese: y = weight x + bias
fun predict(x: Double, p: Params): Double = p.w * x + p.b

// ===============================
// 4. Funções auxiliares
// ===============================

// Calcula erro simples
fun error(yPred: Double, yReal: Double): Double =
    yPred - yReal
    //(yPred - yReal).absoluteValue // [ERROR]

// Calcula gradientes (derivadas do MSE)
fun gradients(x: Double, error: Double, n: Int) = Params(
    (2.0 / n) * error * x,  // delta weight
    (2.0 / n) * error,      // delta bias
)

// Atualiza parâmetros usando gradiente descendente
fun updateParams(p: Params, delta: Params, lr: Double) = Params(
    p.w - lr * delta.w,    // weight
    p.b - lr * delta.b,    // bias
)

// ===============================
// 5. Treino
// ===============================

// Função principal de treino
fun train(
    data: List<House>, // normalizados
    epochs: Int = 3000,
    lr: Double = 0.05
): Params {

    var params = Params(w = 0.0, b = 0.0)

    // Loop de aprendizagem
    repeat(epochs) {
        val total = data.fold(Params(0.0,0.0)) { p, house ->
            val yPred = predict(house.area, params)
            val e = error(yPred, house.price)
            p + gradients(house.area, e, data.size)
        }
        params = updateParams(params, total, lr)
    }
    return params
}

// ===============================
// 6. Execução
// ===============================

fun main() {
    // Normaliza dados
    val (areas, prices, data) = houses.normalize()

    // Treina modelo
    val params = train(data)

    println("=== Modelo treinado ===")
    println("weight = %.3f | bias = %.3f".format(params.w, params.b))

    fun getPriceForArea(area: Int): Long {
        val areaNorm = areas.normalize(area.toDouble())
        val priceNorm = predict(areaNorm, params)
        return prices.denormalize(priceNorm).roundToLong()
    }

    // Previsão para área dentro de um gap
    val area = 110
    val price = getPriceForArea(area)
    println("Preço previsto para casa de $area m²: €$price")
}

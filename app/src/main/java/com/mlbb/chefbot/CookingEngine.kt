package com.mlbb.chefbot

import android.graphics.Path

/**
 * CookingEngine manages the relative screen coordinates and cooking routine
 * for the Undersea Restaurant event in Mobile Legends: Bang Bang.
 *
 * It uses normalized coordinates (0.0 to 1.0) mapped onto landscape screen
 * dimensions so that it works across any device resolution.
 */
class CookingEngine(screenWidth: Int, screenHeight: Int) {

    data class TapAnchor(val relX: Float, val relY: Float)

    @Volatile
    private var width: Float = maxOf(screenWidth, screenHeight).coerceAtLeast(1).toFloat()

    @Volatile
    private var height: Float = minOf(screenWidth, screenHeight).coerceAtLeast(1).toFloat()

    private var cycleCounter: Int = 0

    // 1. Drinks Station (Left)
    private val colaAnchor = TapAnchor(0.12f, 0.22f)
    private val iceCreamAnchor = TapAnchor(0.18f, 0.22f)

    // 2. Fryer Station (Left-Center)
    private val fryerPotatoesAnchor = TapAnchor(0.24f, 0.35f)
    private val fryerChickenAnchor = TapAnchor(0.30f, 0.35f)
    private val fryerTrayFriesAnchor = TapAnchor(0.24f, 0.52f)
    private val fryerTrayChickenAnchor = TapAnchor(0.30f, 0.52f)

    // 3. Pizza Station (Center-Left)
    private val pizzaDoughAnchor = TapAnchor(0.42f, 0.48f)
    private val pizzaToppingOnionAnchor = TapAnchor(0.40f, 0.62f)
    private val pizzaToppingHamAnchor = TapAnchor(0.45f, 0.62f)
    private val pizzaOvenSlot1Anchor = TapAnchor(0.38f, 0.30f)
    private val pizzaOvenSlot2Anchor = TapAnchor(0.44f, 0.30f)
    private val pizzaPlatesAnchor = TapAnchor(0.42f, 0.72f)

    // 4. Burger Grill Station (Center-Right)
    private val rawMeatTrayAnchor = TapAnchor(0.58f, 0.85f)
    private val grillBurner1Anchor = TapAnchor(0.55f, 0.68f)
    private val grillBurner2Anchor = TapAnchor(0.61f, 0.68f)
    private val grillBurner3Anchor = TapAnchor(0.55f, 0.78f)
    private val grillBurner4Anchor = TapAnchor(0.61f, 0.78f)

    private val burgerBunsAnchor = TapAnchor(0.70f, 0.65f)
    private val burgerLettuceAnchor = TapAnchor(0.75f, 0.82f)
    private val burgerTomatoAnchor = TapAnchor(0.70f, 0.82f)
    private val burgerPlate1Anchor = TapAnchor(0.78f, 0.55f)
    private val burgerPlate2Anchor = TapAnchor(0.85f, 0.55f)

    // 5. Customer Service Counter (Top Area)
    private val customer1Anchor = TapAnchor(0.65f, 0.32f)
    private val customer2Anchor = TapAnchor(0.78f, 0.32f)
    private val customer3Anchor = TapAnchor(0.90f, 0.32f)

    val colaDispenser: Path get() = createTap(colaAnchor)
    val iceCreamDispenser: Path get() = createTap(iceCreamAnchor)
    val fryerPotatoes: Path get() = createTap(fryerPotatoesAnchor)
    val fryerChicken: Path get() = createTap(fryerChickenAnchor)
    val fryerTrayFries: Path get() = createTap(fryerTrayFriesAnchor)
    val fryerTrayChicken: Path get() = createTap(fryerTrayChickenAnchor)
    val pizzaDough: Path get() = createTap(pizzaDoughAnchor)
    val pizzaToppingOnion: Path get() = createTap(pizzaToppingOnionAnchor)
    val pizzaToppingHam: Path get() = createTap(pizzaToppingHamAnchor)
    val pizzaOvenSlot1: Path get() = createTap(pizzaOvenSlot1Anchor)
    val pizzaOvenSlot2: Path get() = createTap(pizzaOvenSlot2Anchor)
    val pizzaPlates: Path get() = createTap(pizzaPlatesAnchor)
    val rawMeatTray: Path get() = createTap(rawMeatTrayAnchor)
    val grillBurner1: Path get() = createTap(grillBurner1Anchor)
    val grillBurner2: Path get() = createTap(grillBurner2Anchor)
    val grillBurner3: Path get() = createTap(grillBurner3Anchor)
    val grillBurner4: Path get() = createTap(grillBurner4Anchor)
    val burgerBuns: Path get() = createTap(burgerBunsAnchor)
    val burgerLettuce: Path get() = createTap(burgerLettuceAnchor)
    val burgerTomato: Path get() = createTap(burgerTomatoAnchor)
    val burgerPlate1: Path get() = createTap(burgerPlate1Anchor)
    val burgerPlate2: Path get() = createTap(burgerPlate2Anchor)
    val customer1: Path get() = createTap(customer1Anchor)
    val customer2: Path get() = createTap(customer2Anchor)
    val customer3: Path get() = createTap(customer3Anchor)

    fun updateScreenSize(screenWidth: Int, screenHeight: Int) {
        val landscapeW = maxOf(screenWidth, screenHeight).coerceAtLeast(1).toFloat()
        val landscapeH = minOf(screenWidth, screenHeight).coerceAtLeast(1).toFloat()
        width = landscapeW
        height = landscapeH
    }

    private fun createTap(anchor: TapAnchor): Path {
        val safeW = width.coerceAtLeast(2f)
        val safeH = height.coerceAtLeast(2f)
        val absX = (anchor.relX * safeW).coerceIn(1f, safeW - 1f)
        val absY = (anchor.relY * safeH).coerceIn(1f, safeH - 1f)
        return Path().apply {
            moveTo(absX, absY)
        }
    }

    private fun MutableList<Pair<Path, Long>>.addTap(anchor: TapAnchor, delayMs: Long) {
        add(createTap(anchor) to delayMs)
    }

    private fun MutableList<Pair<Path, Long>>.addDeliveries(dish: TapAnchor, tapDelayMs: Long = 50L) {
        addTap(dish, 60L)
        addTap(customer1Anchor, tapDelayMs)
        addTap(customer2Anchor, tapDelayMs)
        addTap(customer3Anchor, tapDelayMs)
    }

    /**
     * Returns a continuous prioritized sequence of kitchen taps.
     * Keeps all grills and fryers full, dispenses drinks, and serves waiting customers.
     */
    fun getFullKitchenCycle(): List<Pair<Path, Long>> {
        val sequence = mutableListOf<Pair<Path, Long>>()
        val useLowerBurners = (cycleCounter++ % 2) == 1

        // Step 1: Grill Replenishment (Raw meat to 4 burners)
        sequence.addTap(rawMeatTrayAnchor, 50L)
        sequence.addTap(grillBurner1Anchor, 60L)
        sequence.addTap(rawMeatTrayAnchor, 50L)
        sequence.addTap(grillBurner2Anchor, 60L)
        sequence.addTap(rawMeatTrayAnchor, 50L)
        sequence.addTap(grillBurner3Anchor, 60L)
        sequence.addTap(rawMeatTrayAnchor, 50L)
        sequence.addTap(grillBurner4Anchor, 60L)

        // Step 2: Assemble Burgers (alternate burners 1/2 and 3/4 across cycles)
        val primaryBurner = if (useLowerBurners) grillBurner3Anchor else grillBurner1Anchor
        val secondaryBurner = if (useLowerBurners) grillBurner4Anchor else grillBurner2Anchor

        sequence.addTap(burgerBunsAnchor, 60L)
        sequence.addTap(primaryBurner, 60L)
        sequence.addTap(burgerLettuceAnchor, 50L)
        sequence.addTap(burgerTomatoAnchor, 50L)

        sequence.addTap(burgerBunsAnchor, 60L)
        sequence.addTap(secondaryBurner, 60L)
        sequence.addTap(burgerLettuceAnchor, 50L)
        sequence.addTap(burgerTomatoAnchor, 50L)

        // Step 3: Pizza Assembly & Baking
        sequence.addTap(pizzaDoughAnchor, 60L)
        sequence.addTap(pizzaToppingOnionAnchor, 50L)
        sequence.addTap(pizzaToppingHamAnchor, 50L)
        sequence.addTap(pizzaOvenSlot1Anchor, 60L)
        sequence.addTap(pizzaOvenSlot2Anchor, 60L)
        sequence.addTap(pizzaPlatesAnchor, 60L)

        // Step 4: Fryers & Instant Drinks
        sequence.addTap(fryerPotatoesAnchor, 60L)
        sequence.addTap(fryerTrayFriesAnchor, 60L)
        sequence.addTap(fryerChickenAnchor, 60L)
        sequence.addTap(fryerTrayChickenAnchor, 60L)
        sequence.addTap(colaAnchor, 60L)
        sequence.addTap(iceCreamAnchor, 60L)

        // Step 5: Deliver all 6 finished dish types to all 3 customer slots
        sequence.addDeliveries(burgerPlate1Anchor, 60L)
        sequence.addDeliveries(burgerPlate2Anchor, 60L)
        sequence.addDeliveries(pizzaPlatesAnchor, 50L)
        sequence.addDeliveries(fryerTrayFriesAnchor, 50L)
        sequence.addDeliveries(fryerTrayChickenAnchor, 50L)
        sequence.addDeliveries(colaAnchor, 50L)
        sequence.addDeliveries(iceCreamAnchor, 50L)

        return sequence
    }
}

/**
 * MLBB Undersea Restaurant (SpongeBob & Friend Fest) Auto-Bot
 * For AutoX.js / Auto.js on Android (No Root required).
 *
 * Features:
 * - Draggable Floating Overlay Widget [ ▶ / ⏸ ] over Mobile Legends
 * - Pre-cook continuous buffer loop (keeps grills, fryers, pizza, drinks full)
 * - Auto-serves all 6 dishes across all 3 customer slots
 */

auto.waitFor();

// 1. Create Floating Overlay Controls over MLBB
var w = floaty.window(
    <frame gravity="center">
        <linear bg="#E60F172A" padding="8" orientation="horizontal" gravity="center_vertical">
            <text id="actionText" text="🍳 MLBB БОТ: ВЫКЛ" textColor="#10B981" textSize="12sp" textStyle="bold" paddingRight="6"/>
            <button id="toggleBtn" text="▶ СТАРТ" w="78" h="36" textColor="#FFFFFF" bg="#10B981" textSize="11sp" style="Widget.AppCompat.Button.Borderless" padding="0"/>
            <button id="closeBtn" text="✕" w="36" h="36" textColor="#EF4444" bg="#1E293B" textSize="14sp" style="Widget.AppCompat.Button.Borderless" padding="0" marginLeft="4"/>
        </linear>
    </frame>
);

// Position near top edge so it doesn't cover Cola/Ice Cream dispensers at y ~ 0.22h
w.setPosition(24, 16);

var isRunning = false;
var isAlive = true;

// Make status label draggable
var dragX = 0;
var dragY = 0;
var winX = 0;
var winY = 0;
w.actionText.setOnTouchListener(function(view, event) {
    switch (event.getActionMasked()) {
        case android.view.MotionEvent.ACTION_DOWN:
            dragX = event.getRawX();
            dragY = event.getRawY();
            winX = w.getX();
            winY = w.getY();
            return true;
        case android.view.MotionEvent.ACTION_MOVE:
            var nextX = Math.max(0, Math.round(winX + (event.getRawX() - dragX)));
            var nextY = Math.max(0, Math.round(winY + (event.getRawY() - dragY)));
            w.setPosition(nextX, nextY);
            return true;
    }
    return false;
});

w.toggleBtn.click(function() {
    isRunning = !isRunning;
    if (isRunning) {
        w.actionText.setText("⚡ БОТ ГОТОВИТ");
        w.actionText.setTextColor(colors.parseColor("#F59E0B"));
        w.toggleBtn.setText("⏸ ПАУЗА");
        w.toggleBtn.attr("bg", "#EF4444");
        toast("Бот запущен! Готовьте заказы в MLBB");
    } else {
        w.actionText.setText("🍳 MLBB БОТ: ПАУЗА");
        w.actionText.setTextColor(colors.parseColor("#10B981"));
        w.toggleBtn.setText("▶ СТАРТ");
        w.toggleBtn.attr("bg", "#10B981");
        toast("Бот на паузе");
    }
});

w.closeBtn.click(function() {
    isRunning = false;
    isAlive = false;
    w.close();
    threads.shutDownAll();
    exit();
});

// Normalized kitchen coordinates (Landscape)
var width = Math.max(device.width, device.height);
var height = Math.min(device.width, device.height);

function isLandscape() {
    return context.getResources().getConfiguration().orientation === 2;
}

function tapRel(rx, ry, sleepMs) {
    if (!isRunning || !isAlive) return;
    var x = Math.max(1, Math.min(width - 1, Math.round(rx * width)));
    var y = Math.max(1, Math.min(height - 1, Math.round(ry * height)));
    // Explicit 45ms press ensures valid StrokeDescription duration > 0 and reliable game input
    press(x, y, 45);
    sleep(sleepMs);
}

function deliverToCustomers(rx, ry) {
    tapRel(rx, ry, 50);
    tapRel(0.65, 0.32, 50); // Customer 1
    tapRel(0.78, 0.32, 50); // Customer 2
    tapRel(0.90, 0.32, 50); // Customer 3
}

var cycleIndex = 0;

// Main Kitchen Cooking Loop
threads.start(function() {
    while (isAlive) {
        if (isRunning && isLandscape()) {
            var useLowerBurners = (cycleIndex++ % 2) === 1;

            // 1. Replenish Burger Grill (Raw meat to 4 burners)
            tapRel(0.58, 0.85, 50); // Raw meat
            tapRel(0.55, 0.68, 60); // Burner 1
            tapRel(0.58, 0.85, 50);
            tapRel(0.61, 0.68, 60); // Burner 2
            tapRel(0.58, 0.85, 50);
            tapRel(0.55, 0.78, 60); // Burner 3
            tapRel(0.58, 0.85, 50);
            tapRel(0.61, 0.78, 60); // Burner 4

            // 2. Assemble Burgers (alternate burners 1/2 and 3/4)
            var burgerBurnerY = useLowerBurners ? 0.78 : 0.68;

            tapRel(0.70, 0.65, 50); // Bun
            tapRel(0.55, burgerBurnerY, 50); // Meat 1/3
            tapRel(0.75, 0.82, 40); // Lettuce
            tapRel(0.70, 0.82, 40); // Tomato

            tapRel(0.70, 0.65, 50); // Bun
            tapRel(0.61, burgerBurnerY, 50); // Meat 2/4
            tapRel(0.75, 0.82, 40); // Lettuce
            tapRel(0.70, 0.82, 40); // Tomato

            // 3. Pizza Station (Dough + onion + ham -> oven -> plate)
            tapRel(0.42, 0.48, 50); // Dough
            tapRel(0.40, 0.62, 40); // Onion
            tapRel(0.45, 0.62, 40); // Ham
            tapRel(0.38, 0.30, 50); // Oven slot 1
            tapRel(0.44, 0.30, 50); // Oven slot 2
            tapRel(0.42, 0.72, 50); // Baked pizza to plate

            // 4. Fryer & Drinks
            tapRel(0.24, 0.35, 50); // Raw fries
            tapRel(0.24, 0.52, 50); // Cooked fries tray
            tapRel(0.30, 0.35, 50); // Raw chicken
            tapRel(0.30, 0.52, 50); // Cooked chicken tray
            tapRel(0.12, 0.22, 50); // Cola dispenser
            tapRel(0.18, 0.22, 50); // Ice cream dispenser

            // 5. Deliver all 6 finished dishes to all 3 customer slots
            deliverToCustomers(0.78, 0.55); // Burger 1
            deliverToCustomers(0.85, 0.55); // Burger 2
            deliverToCustomers(0.42, 0.72); // Pizza
            deliverToCustomers(0.24, 0.52); // Fries
            deliverToCustomers(0.30, 0.52); // Fried chicken
            deliverToCustomers(0.12, 0.22); // Cola
            deliverToCustomers(0.18, 0.22); // Ice cream

            sleep(100);
        } else {
            sleep(300);
        }
    }
});

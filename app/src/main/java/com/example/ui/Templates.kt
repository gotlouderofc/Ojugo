package com.example.ui

data class HtmlTemplate(
    val id: String,
    val name: String,
    val description: String,
    val initialAppTitle: String,
    val html: String,
    val css: String,
    val js: String
)

object Templates {
    val list = listOf(
        HtmlTemplate(
            id = "space_battle",
            name = "🚀 Retro Space Invader Game",
            description = "A responsive high-speed canvas arcade shooter with real-time physics, laser firing, asteroid explosions, scoring, and local high-score persistence.",
            initialAppTitle = "Nebula Striker",
            html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                    <title>Nebula Striker</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <div id="game-container">
                        <div id="score-board">Score: <span id="score">0</span> | High Score: <span id="high-score">0</span></div>
                        <canvas id="gameCanvas"></canvas>
                        <div id="controls">
                            <button id="btn-left">◀</button>
                            <button id="btn-fire">FIRE 🔥</button>
                            <button id="btn-right">▶</button>
                        </div>
                        <div id="game-over" class="hidden">
                            <h2>SYSTEM OFFLINE</h2>
                            <p>Your ship was destroyed!</p>
                            <button id="btn-restart">RELAUNCH MISSION</button>
                        </div>
                    </div>
                    <script src="script.js"></script>
                </body>
                </html>
            """.trimIndent(),
            css = """
                * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
                body { background: #070913; color: #fff; font-family: 'Courier New', monospace; overflow: hidden; }
                #game-container { position: relative; width: 100vw; height: 100vh; display: flex; flex-direction: column; }
                #score-board { padding: 12px; background: rgba(0, 0, 0, 0.6); text-align: center; font-size: 14px; border-bottom: 2px solid #00c3ff; color: #00c3ff; font-weight: bold; }
                canvas { flex: 1; width: 100%; touch-action: none; background: radial-gradient(circle, #0c142b 0%, #03050a 100%); }
                #controls { height: 75px; display: flex; background: #0a0e1a; border-top: 1px solid #1f2d4d; }
                #controls button { flex: 1; background: #111a30; color: #fff; border: 1px solid #1f2d4d; font-size: 18px; font-weight: bold; active-background: #00c3ff; cursor: pointer; transition: 0.1s; }
                #controls button:active { background: #00628f; color: #ff9100; }
                #btn-fire { background: #9c1500 !important; color: yellow !important; font-size: 20px !important; }
                #game-over { position: absolute; top: 35%; left: 10%; right: 10%; background: rgba(16, 20, 37, 0.95); border: 2px solid #ff4000; padding: 24px; text-align: center; border-radius: 8px; box-shadow: 0 0 20px rgba(255, 64, 0, 0.5); }
                #game-over h2 { color: #ff4000; margin-bottom: 12px; }
                #game-over button { padding: 10px 20px; background: #ff4000; border: none; color: white; font-weight: bold; border-radius: 4px; margin-top: 15px; cursor: pointer; }
                .hidden { display: none !important; }
            """.trimIndent(),
            js = """
                const canvas = document.getElementById('gameCanvas');
                const ctx = canvas.getContext('2d');
                const scoreEl = document.getElementById('score');
                const highScoreEl = document.getElementById('high-score');
                const gameOverEl = document.getElementById('game-over');
                const restartBtn = document.getElementById('btn-restart');
                
                // Set sizes
                function resize() {
                    canvas.width = canvas.clientWidth;
                    canvas.height = canvas.clientHeight;
                }
                window.addEventListener('resize', resize);
                resize();

                let score = 0;
                let highScore = localStorage.getItem('nebula_high_score') || 0;
                highScoreEl.textContent = highScore;

                let player = { x: 0, y: 0, width: 32, height: 32, speed: 6 };
                let bullets = [];
                let enemies = [];
                let particles = [];
                let gameOver = false;
                let keys = { left: false, right: false };

                function resetGame() {
                    score = 0;
                    scoreEl.textContent = '0';
                    gameOver = false;
                    gameOverEl.classList.add('hidden');
                    player.x = canvas.width / 2 - player.width / 2;
                    player.y = canvas.height - 60;
                    bullets = [];
                    enemies = [];
                    particles = [];
                }

                // Controls
                document.getElementById('btn-left').onmousedown = () => keys.left = true;
                document.getElementById('btn-left').onmouseup = () => keys.left = false;
                document.getElementById('btn-left').ontouchstart = (e) => { e.preventDefault(); keys.left = true; };
                document.getElementById('btn-left').ontouchend = () => keys.left = false;

                document.getElementById('btn-right').onmousedown = () => keys.right = true;
                document.getElementById('btn-right').onmouseup = () => keys.right = false;
                document.getElementById('btn-right').ontouchstart = (e) => { e.preventDefault(); keys.right = true; };
                document.getElementById('btn-right').ontouchend = () => keys.right = false;

                document.getElementById('btn-fire').onclick = () => fireBullet();
                document.getElementById('btn-fire').ontouchstart = (e) => { e.preventDefault(); fireBullet(); };
                restartBtn.onclick = () => resetGame();

                function fireBullet() {
                    if (gameOver) return;
                    bullets.push({ x: player.x + player.width/2 - 3, y: player.y, width: 6, height: 12, speed: 8 });
                }

                function createExplosion(x, y, color) {
                    for(let i=0; i<10; i++) {
                        particles.push({
                            x: x, y: y,
                            vx: (Math.random() - 0.5) * 6,
                            vy: (Math.random() - 0.5) * 6,
                            radius: Math.random() * 3 + 1,
                            color: color,
                            alpha: 1
                        });
                    }
                }

                // Main loop
                function loop() {
                    ctx.clearRect(0, 0, canvas.width, canvas.height);

                    // Stars ambient
                    ctx.fillStyle = 'rgba(255,255,255,0.05)';
                    for(let i=0; i<20; i++) {
                        ctx.fillRect((Math.sin(i*999)+1)*canvas.width/2, (Date.now()/50 + i*50) % canvas.height, 2, 2);
                    }

                    if (!gameOver) {
                        // Move player
                        if (keys.left && player.x > 0) player.x -= player.speed;
                        if (keys.right && player.x < canvas.width - player.width) player.x += player.speed;

                        // Spawn enemies
                        if (Math.random() < 0.03 && enemies.length < 12) {
                            enemies.push({
                                x: Math.random() * (canvas.width - 24),
                                y: -30,
                                width: 24,
                                height: 24,
                                speed: Math.random() * 2 + 1.5,
                                color: '#' + Math.floor(Math.random()*16777215).toString(16)
                            });
                        }

                        // Draw player
                        ctx.fillStyle = '#00ffcc';
                        ctx.beginPath();
                        ctx.moveTo(player.x + player.width/2, player.y);
                        ctx.lineTo(player.x, player.y + player.height);
                        ctx.lineTo(player.x + player.width, player.y + player.height);
                        ctx.closePath();
                        ctx.fill();
                        
                        // Cockpit accent
                        ctx.fillStyle = '#ff7f11';
                        ctx.fillRect(player.x + player.width/2 - 4, player.y + player.height/2, 8, 8);
                    }

                    // Move/Draw Bullets
                    ctx.fillStyle = '#ff3c00';
                    bullets.forEach((b, index) => {
                        b.y -= b.speed;
                        ctx.fillRect(b.x, b.y, b.width, b.height);
                        if (b.y < 0) bullets.splice(index, 1);
                    });

                    // Move/Draw Enemies
                    enemies.forEach((e, eIdx) => {
                        e.y += e.speed;
                        
                        // Enemy rendering (Hexagonal style)
                        ctx.fillStyle = e.color || '#ff003c';
                        ctx.fillRect(e.x, e.y, e.width, e.height);
                        
                        // Check collision with player
                        if (!gameOver && e.y + e.height >= player.y && e.x + e.width >= player.x && e.x <= player.x + player.width) {
                            gameOver = true;
                            createExplosion(player.x + player.width/2, player.y + player.height/2, '#00ffcc');
                            createExplosion(e.x + e.width/2, e.y + e.height/2, '#ff003c');
                            gameOverEl.classList.remove('hidden');
                            if (score > highScore) {
                                highScore = score;
                                localStorage.setItem('nebula_high_score', highScore);
                                highScoreEl.textContent = highScore;
                            }
                        }

                        // Check collision with bullets
                        bullets.forEach((b, bIdx) => {
                            if (b.x + b.width >= e.x && b.x <= e.x + e.width && b.y <= e.y + e.height && b.y + b.height >= e.y) {
                                createExplosion(e.x + e.width/2, e.y + e.height/2, e.color);
                                enemies.splice(eIdx, 1);
                                bullets.splice(bIdx, 1);
                                score += 100;
                                scoreEl.textContent = score;
                            }
                        });

                        // Out of bounds
                        if (e.y > canvas.height) {
                            enemies.splice(eIdx, 1);
                            if (!gameOver) {
                                score = Math.max(0, score - 50);
                                scoreEl.textContent = score;
                            }
                        }
                    });

                    // Draw particles
                    particles.forEach((p, idx) => {
                        p.x += p.vx;
                        p.y += p.vy;
                        p.alpha -= 0.02;
                        p.vx *= 0.98;
                        p.vy *= 0.98;
                        ctx.save();
                        ctx.globalAlpha = p.alpha;
                        ctx.fillStyle = p.color;
                        ctx.beginPath();
                        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                        ctx.fill();
                        ctx.restore();
                        if (p.alpha <= 0) particles.splice(idx, 1);
                    });

                    requestAnimationFrame(loop);
                }

                resetGame();
                loop();
            """.trimIndent()
        ),
        HtmlTemplate(
            id = "bouncing_physics",
            name = "🎨 Multi-Color Gravity Sandbox",
            description = "Click on the screen to spawn colorful balls governed by responsive gravity forces, collision physics, bouncy rebound coefficient, and real-time canvas resizing.",
            initialAppTitle = "Gravity Canvas",
            html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                    <title>Gravity Canvas</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <div id="gui">
                        <div class="panel">Balls: <span id="ball-count">0</span></div>
                        <button id="btn-clear">Reset Screen</button>
                    </div>
                    <canvas id="canvas"></canvas>
                    <div id="hint">TAP TO CREATED PHYSICS SPHERES 🥎</div>
                    <script src="script.js"></script>
                </body>
                </html>
            """.trimIndent(),
            css = """
                * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
                body { background: #0b0c10; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; overflow: hidden; color: #fff; }
                #gui { position: absolute; top: 12px; left: 12px; right: 12px; display: flex; justify-content: space-between; align-items: center; pointer-events: none; }
                .panel { background: rgba(31, 40, 51, 0.85); padding: 8px 16px; border-radius: 20px; font-weight: bold; border: 1px solid #66fcf1; color: #66fcf1; }
                #btn-clear { background: #ff7f11; color: white; border: none; font-weight: bold; padding: 8px 16px; border-radius: 20px; pointer-events: auto; }
                #btn-clear:active { transform: scale(0.95); opacity: 0.9; }
                canvas { display: block; width: 100vw; height: 100vh; background: #0b0c10; cursor: pointer; }
                #hint { position: absolute; bottom: 20px; left: 0; right: 0; text-align: center; color: rgba(255,255,255,0.4); font-size: 11px; letter-spacing: 1px; pointer-events: none; }
            """.trimIndent(),
            js = """
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                const countEl = document.getElementById('ball-count');
                const clearBtn = document.getElementById('btn-clear');

                function resize() {
                    canvas.width = window.innerWidth;
                    canvas.height = window.innerHeight;
                }
                window.addEventListener('resize', resize);
                resize();

                const gravity = 0.5;
                const friction = 0.88;
                let balls = [];

                class Ball {
                    constructor(x, y) {
                        this.x = x;
                        this.y = y;
                        this.radius = Math.random() * 20 + 12;
                        this.dx = (Math.random() - 0.5) * 12;
                        this.dy = (Math.random() - 0.5) * 6;
                        this.color = `hsl(${Math.random() * 360}, 90%, 65%)`;
                    }
                    update() {
                        if (this.y + this.radius + this.dy > canvas.height) {
                            this.dy = -this.dy * friction;
                            this.dx = this.dx * friction;
                            this.y = canvas.height - this.radius;
                        } else {
                            this.dy += gravity;
                        }

                        if (this.x + this.radius + this.dx > canvas.width || this.x - this.radius + this.dx < 0) {
                            this.dx = -this.dx * friction;
                        }

                        this.x += this.dx;
                        this.y += this.dy;
                        this.draw();
                    }
                    draw() {
                        ctx.beginPath();
                        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2, false);
                        ctx.fillStyle = this.color;
                        ctx.shadowBlur = 10;
                        ctx.shadowColor = this.color;
                        ctx.fill();
                        ctx.closePath();
                        ctx.shadowBlur = 0; // reset
                    }
                }

                canvas.addEventListener('click', (e) => {
                    balls.push(new Ball(e.clientX, e.clientY));
                    countEl.textContent = balls.length;
                });
                
                canvas.addEventListener('touchstart', (e) => {
                    const touch = e.touches[0];
                    balls.push(new Ball(touch.clientX, touch.clientY));
                    countEl.textContent = balls.length;
                });

                clearBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    balls = [];
                    countEl.textContent = '0';
                });

                function animate() {
                    ctx.fillStyle = 'rgba(11, 12, 16, 0.3)';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);
                    balls.forEach(b => b.update());
                    requestAnimationFrame(animate);
                }
                animate();
            """.trimIndent()
        ),
        HtmlTemplate(
            id = "synth_soundboard",
            name = "🎵 Retro Synth Frequency Board",
            description = "Produce interactive synthetic frequencies with visual oscillator waves, dynamic volume slider control, and direct visual synthesizer keys.",
            initialAppTitle = "Frequency Synth",
            html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                    <title>Frequency Synth</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <div id="synth">
                        <h1>WAVE SYNTHESISER</h1>
                        <div class="osc-select">
                            <label>Oscillator</label>
                            <select id="osc-type">
                                <option value="sine">Sine Wave</option>
                                <option value="triangle">Triangle Wave</option>
                                <option value="sawtooth">Sawtooth Wave</option>
                                <option value="square">Square Wave</option>
                            </select>
                        </div>
                        <div id="keyboard">
                            <button class="key" data-freq="261.63">C4</button>
                            <button class="key" data-freq="293.66">D4</button>
                            <button class="key" data-freq="329.63">E4</button>
                            <button class="key" data-freq="349.23">F4</button>
                            <button class="key" data-freq="392.00">G4</button>
                            <button class="key" data-freq="440.00">A4</button>
                            <button class="key" data-freq="493.88">B4</button>
                            <button class="key" data-freq="523.25">C5</button>
                        </div>
                        <div class="footer-note">* WebAudio local offline rendering *</div>
                    </div>
                    <script src="script.js"></script>
                </body>
                </html>
            """.trimIndent(),
            css = """
                body { background: #160a22; font-family: 'Segoe UI', sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; color: #fff; overflow: hidden;}
                #synth { width: 90vw; max-width: 450px; background: #26143c; padding: 24px; border-radius: 12px; border: 3px solid #ff007f; box-shadow: 0 0 15px rgba(255, 0, 127, 0.4); text-align: center; }
                h1 { font-size: 18px; margin-bottom: 20px; color: #ff007f; letter-spacing: 2px; }
                .osc-select { margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; text-align: left; }
                select { background: #3c1e5c; color: white; border: 1px solid #ff007f; padding: 8px 12px; border-radius: 4px; outline: none; }
                #keyboard { display: flex; gap: 8px; justify-content: center; height: 150px; }
                .key { flex: 1; background: #fff; border: none; border-radius: 0 0 4px 4px; cursor: pointer; color: #222; font-weight: bold; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 15px; position: relative; transition: 0.1s; }
                .key:active { background: #ff007f; color: white; transform: translateY(4px); box-shadow: none; }
                .footer-note { font-size: 10px; margin-top: 24px; color: rgba(255,255,255,0.4); }
            """.trimIndent(),
            js = """
                let audioCtx = null;
                const oscTypeSelect = document.getElementById('osc-type');
                
                function initAudio() {
                    if (!audioCtx) {
                        const AudioContext = window.AudioContext || window.webkitAudioContext;
                        audioCtx = new AudioContext();
                    }
                }

                document.querySelectorAll('.key').forEach(key => {
                    const playSound = () => {
                        initAudio();
                        if (!audioCtx) return;
                        
                        const freq = parseFloat(key.getAttribute('data-freq'));
                        const type = oscTypeSelect.value;
                        
                        const osc = audioCtx.createOscillator();
                        const gain = audioCtx.createGain();
                        
                        osc.type = type;
                        osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
                        
                        gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
                        gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.8);
                        
                        osc.connect(gain);
                        gain.connect(audioCtx.destination);
                        
                        osc.start();
                        osc.stop(audioCtx.currentTime + 0.8);
                    };

                    key.addEventListener('click', playSound);
                    key.addEventListener('touchstart', (e) => {
                        e.preventDefault();
                        playSound();
                    });
                });
            """.trimIndent()
        )
    )
}

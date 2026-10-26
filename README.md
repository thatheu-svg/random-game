# Berry Blast

A top-down survival game written in Java with Swing. The player moves around a screen, places damaging puddles, and survives waves of enemies that chase the player.

## Requirements

- Java 14 or newer (the code uses switch expressions)
- Two image files in the project's top-level folder, next to the `.java` files:
  - `player.png`
  - `galaxy_puddle.png`

If an image is missing, the game still runs. The player is drawn as a colored circle and puddles are drawn as a translucent blue circle. The missing file's path is printed to the console.

## Running

From the project folder:

```
javac Enemy.java FastEnemy.java TankEnemy.java Player.java
java Player
```

The images are loaded from the folder the program is run from.

## Controls

| Input | Action |
|---|---|
| W, A, S, D | Move |
| Left mouse button | Place a puddle |
| R | Restart (only works after game over) |

## How the game works

### Puddles

- Clicking places a puddle at the mouse position. If the click is farther than 500 pixels from the player, the puddle is placed 500 pixels away in the direction of the click.
- A puddle does not land immediately. A warning outline is shown first. The delay is 10 frames for a click at the player's feet and up to 60 frames at maximum range.
- When a puddle lands, it damages every enemy inside it once. After that it damages enemies inside it every 30 frames.
- A puddle lasts 300 frames (5 seconds) after landing.
- Puddles can be placed once every 30 frames.
- Standing in a landed puddle heals the player 20 HP once per second, up to the maximum.

### Player

- 100 HP.
- Touching an enemy deals 40 damage, followed by 60 frames of immunity to contact damage. The 40 is a testing value.
- At 0 HP the game ends and stops updating. Press R to restart.

### Enemies

- Enemies spawn just outside a random edge of the screen and move toward the player.
- Each enemy has a health bar and flashes red briefly when damaged.
- Enemies spawn in groups. Spawn rate, group size (1 to 4), and speed increase over 20 minutes of play. At most 20 enemies exist at once.
- A kill counter is shown on screen.

## Files

| File | Contents |
|---|---|
| `Player.java` | The window, the game loop, input, puddles, spawning, and drawing. Despite the name, this file holds the whole game. |
| `Enemy.java` | The base enemy class: position, HP, movement, collision checks, and drawing. |
| `FastEnemy.java` | Subclass of `Enemy` with lower HP, smaller size, and higher speed. |
| `TankEnemy.java` | Subclass of `Enemy` with higher HP, larger size, and lower speed. |

## Settings

Gameplay values are constants at the top of `PlayerPanel` in `Player.java`. Timers are measured in frames. The game runs at about 60 frames per second (one frame every 16 ms).

## Current status

Working:
- Movement, puddle placement with range limit and landing delay, damage pulses, healing
- Enemy spawning with difficulty scaling
- Enemy and player health bars, hit flash, contact damage, game over, restart

In progress:
- Enemy types. `FastEnemy` and `TankEnemy` exist, and `Enemy` takes size, damage, and color in its constructor. The constructor does not use those values yet, and `handleSpawning` does not spawn the subclasses yet.

Not started:
- Upgrades
- Saved high scores

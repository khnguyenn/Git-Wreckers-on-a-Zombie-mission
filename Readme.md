# Git Wreckers : Zombie Mission Simulation  

## Overview
Git Wreckers is a 2D Java simulation of a community responding to a zombie outbreak. Humans seek food and shelter, zombies hunt humans, and military units patrol and shoot zombies.

## Project Concepts
- **Zombies** pursue nearby humans and remove them from the simulation on contact
- **Humans** move, seek shelter, and consume food to maintain their energy
- **Military** patrol and can kill zombies
- Behaviour emerges from local agent interactions
- Keyboard controls start, pause, reset, and adjust simulation speed

## Features
- World Stimulation
- Tick Loop
- Human movement and survival
- Zombie infection and chasing
- Military patrol and shooting
- Buildings and shelter
- Food/resources
- Population statistics
- Start, pause, reset, and speed controls

## Class Structures

[View the UML diagram](https://drive.google.com/file/d/1-3FDkD7lQL_UJ8YTpuyqcfL1BkNXXclc/view?usp=sharing)

## Team Responsibilities
Use the agreed division:

| Area | Responsibility | Member | Student ID |
|---|---|---|---|
| World/Main | Tick loop, spawning, and neighbour queries | Tran Khoi Nguyen (Kian) Nguyen| 48769266 |
| Human/Food | Energy, starvation, fleeing, and shelter | | |
| Zombie | Infection, chasing, and target selection | | |
| Military/MovementBehaviour | Patrol, shooting, and strategy pattern | Neev Patel | 48521558 |
| Building/SimPanel | Occupancy, rendering, and statistics | | |

## Rules 
- Zombies chase nearby humans.
- Humans attempt to survive by moving, finding food, and seeking shelter.
- Zombies remove humans from the simulation when they catch them.
- Military units patrol and shoot zombies.
- The simulation progresses automatically through repeated ticks.
- Population changes are caused by interactions between entities.

## Requirements
- Git
- Java Development Kit (JDK 11 or later)

### How to run
1. Clone this project and move to this project
```bash
git clone https://github.com/khnguyenn/Git-Wreckers-on-a-Zombie-mission

cd Git-Wreckers-on-a-Zombie-mission
```
2. Compile the Java source files.
```bash
javac -d out src/*.java
```

3. Run the `Main` class
```bash
java -cp "out;src" Main
```

On macOS or Linux, use a colon instead of a semicolon: `java -cp "out:src" Main`.
The `src` entry makes the PNG image assets available at runtime.

4. Check the Java version if needed.
```bash 
java -version
javac -version
```

## References
- COMP2000 course materials
- Image assets supplied with the project and COMP2000 course materials

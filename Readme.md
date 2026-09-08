# Git Wreckers : Zombie Mission Simulation  

## Overview
What happens when a group of zombie infection spreads through a living community? Git Wreckers explores this through a 2D zombie outbreak simulation developed in Java, where humans search for food,shelter, weapons, in while zombies hunt and infect humans and military units attempt to control the outbreak. Without direct player control, the interactions between agents can create unexpected outcomes such as large outbreaks or naturally formed safe zones.

## Project Concepts
- **Zombies** pursue nearby humans and infect on contact
- **Humans** move, flee, seek shelter, and consume food or find a guns to protect themselves
- **Military** patrol and can kill zombies
- Behaviour emerges from local agent interactions
- **No player input.** Every agent follows simple local rules, so the outbreak curve and the formation of safe zones emerge from the interactions themselves

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
- Humans attempt to survive by moving, finding food, and seeking shelter, or having a gun to protect themselves.
- Zombies infect humans when they catch them.
- Military units patrol and shoot zombies.
- The simulation progresses automatically through repeated ticks.
- Population changes are caused by interactions between entities.

## Requirements
- Git
- Java Development Kit (JDK 11 or later)

### How to run
1. Clone this project and move to this project
```bash
git clone https://github.com/ShahriarHasan001533/Git-Wreckers-on-a-Zombie-mission

cd Git-Wreckers-on-a-Zombie-mission
cd COMP2000
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

4. Make sure already have image folder and check the version
```bash 
java -version
javac -version
```

## References
- COMP2000 course materials
- Image sources, references, etc (MUST FILLED)

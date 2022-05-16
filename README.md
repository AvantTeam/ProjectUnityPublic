# Project Unity

[![Discord](https://img.shields.io/discord/782583108473978880.svg?color=7289da&label=AvantTeam&logo=discord&style=flat-square)](https://discord.gg/V6ygvgGVqE)

![banner](https://user-images.githubusercontent.com/68240084/150622624-8b7f03c4-92da-4bac-9ab2-d3d9a988b58f.png)

A work-in-progress mega-collaboration [Mindustry](https://github.com/Anuken/Mindustry/) mod, created by the Avant Team.

As the repository description says, this is not a fully published workspace of the mod. Most of the content that is finished or
worth showing is released here, while the critically secret or completely unfinished one remains private. This mod is not intended to
be played by the average player-base as of now; this only aims for additional developing hands, suggestions, and balancing feedback.

## Contributing

You can contribute to development by 

- Submitting bug reports
- Suggesting new content (The issue tracker is **not** for suggestions, visit the [discord server](https://discord.gg/V6ygvgGVqE) instead.)
- Providing input regarding content balancing

## Compiling

1. Clone repository.
   ```
   git clone -b Youngcha-part-2 --single-branch https://github.com/AvantTeam/ProjectUnityPublic
   ```

2. Pack sprites. (Only necessary if new sprites are added)
   ```
   gradlew tools:proc
   ```

3. Build.
   ```
   gradlew main:deploy
   ```

Resulting `.jar` file should be in `main/build/libs/`

# ESU

Bukkit & Velocity plugin that helps with managing your server, provides a better gameplay environment.

For end users, please learn more and download at [**Modrinth** page](https://modrinth.com/plugin/esu).

## Dependencies

This project uses the following core dependencies to facilitate development. Thanks a lot!
- [Cloud-minecraft](https://github.com/Incendo/cloud-minecraft): Command framework.
- [Adventure](https://github.com/KyoriPowered/adventure): User interface library.
- [Configurate](https://github.com/SpongePowered/Configurate): Configuration library.
- [Exposed](https://www.jetbrains.com/exposed/): Kotlin SQL library.
- [AutoRenamingTool](https://projects.neoforged.net/neoforged/autorenamingtool): Jar Remapper. For adaptive NMS support.

## 🔨 Building

1. Install Git, Gradle 8.14+, Java 21.
2. Clone the repository:
   ```bash
   git clone https://github.com/Rothes/ESU.git
   cd ESU
   ```
3. Execute command to build:
   ```bash
   gradle shadowJar
   ```
4. Locate the artifacts under `bukkit/build/libs` and `velocity/build/libs`.


## 🤝 Contributing

ESU is licensed under [GNU Lesser General Public License v3](https://www.gnu.org/licenses/lgpl-3.0.en.html).

Pull requests are welcome! If you would like to contribute to this project, please feel free to do that. \
We would communicate with you proactively to ensure your pull request meets the project standards!

### 🔧 New Features & Bug Fixes

If your PR about a short bug fix, it will likely be merged.
If you are going to add a new feature, please contact us on [Discord](https://discord.gg/zwzzkmYCBb) in advance,
so we can confirm that this feature is on the right track for project and provide with some guidance.

### 🌏 Translations

You can submit your translations to us. We really appreciate that!

We will review the quality of the translated text and then add it to our repository. \
Before doing translation, please familiarize yourself with our resource format:

#### Resource path

We will locate the resource files based on the location of file relative to the plugin's data directory.

Refer to [Bukkit module lang](https://github.com/Rothes/ESU/tree/master/bukkit/src/main/resources/lang/modules)
to understand the resource file storage format.

Besides, please ensure that the translation resources you are adding is located under the correct subproject.
For example, if you are adding translation for velocity modules, then go velocity subproject.

#### Lang folder translations

If you want to add a language resource to the lang directory, simply copy the en_us.yml file and modify it.

#### Config file translations

We support comment translations for config files.
We have a unique mechanism for this, so please make sure you understand the following rules:

- Only comments modified entries are kept.
- The value of entry should be set to `~`.

Refer to [Plugin config lang](https://github.com/Rothes/ESU/blob/master/core/src/main/resources/lang/config.yml/zh_cn.yml) for a practical demonstration.
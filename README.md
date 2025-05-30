# AEM VLT IntelliJ Plugin

[![Build](https://github.com/koha26/aem-vlt-intellij-plugin/workflows/Build/badge.svg)](https://github.com/koha26/aem-vlt-intellij-plugin/actions)
[![Version](https://img.shields.io/jetbrains/plugin/v/27469-aem-vlt-tool.svg)](https://plugins.jetbrains.com/plugin/27469-aem-vlt-tool)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/27469-aem-vlt-tool.svg)](https://plugins.jetbrains.com/plugin/27469-aem-vlt-tool)
[![License](https://img.shields.io/github/license/koha26/aem-vlt-intellij-plugin)](https://github.com/koha26/aem-vlt-intellij-plugin/blob/main/LICENSE)

## 📋 Overview
<!-- Plugin description -->
The AEM VLT IntelliJ Plugin is an integration tool for developers working with Adobe Experience Manager (AEM). It provides a seamless way to push data (files, configurations) from your project (file system) to the AEM repository and pull data from the AEM repository to your project on the file system.

This plugin relies on the Apache Jackrabbit File Vault library to handle content synchronization between your local environment and AEM instances.

## ✨ Features

- **Push to AEM**: Easily push content from your local file system to AEM repository
- **Pull from AEM**: Quickly pull content from AEM repository to your local file system
- **Multiple Server Support**: Configure and manage multiple AEM server connections
- **Progress Tracking**: Visual progress indicators for push and pull operations
- **Detailed Results**: Get detailed operation results with statistics on added, updated, and deleted files
- **Context Menu Integration**: Access AEM operations directly from the project view context menu

## 🔧 Installation

### Using the IDE built-in plugin system:

1. Open your IntelliJ IDEA
2. Navigate to `Settings/Preferences` → `Plugins` → `Marketplace`
3. Search for `"AEM VLT Tool"`
4. Click `Install`

### Using JetBrains Marketplace:

1. Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/27469-aem-vlt-tool)
2. Click the `Install to ...` button

### Manual Installation:

1. Download the [latest release](https://github.com/koha26/aem-vlt-intellij-plugin/releases/latest)
2. In IntelliJ IDEA, go to `Settings/Preferences` → `Plugins` → `⚙️` → `Install plugin from disk...`
3. Select the downloaded plugin file

## 🚀 Usage

### Configuration

1. Go to `Settings/Preferences` > `AEM VLT Settings`
2. Add your AEM server configurations:
   - Server Name (e.g AEM Author)
   - URL
   - Username and Password
   - Set as default (optional)

### Push Content to AEM

1. Right-click on a file or directory in the Project view
2. Select `AEM VLT` → `Push to AEM (Default)`
3. If multiple servers are configured, select the target server
4. The plugin will push the content to the corresponding path in AEM

### Pull Content from AEM

1. Right-click on a file or directory in the Project view
2. Select `AEM VLT` → `Pull from AEM (Default)`
3. If multiple servers are configured, select the source server
4. The plugin will pull the content from the corresponding path in AEM
<!-- Plugin description end -->

## 🤝 Contributing

Contributions are welcome! If you'd like to contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgements

- [Apache Jackrabbit FileVault](https://jackrabbit.apache.org/filevault/)
- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html)

---

Developed by [Kostiantyn Diachenko](https://github.com/koha26) [(Adobe Community Advisor)](https://experienceleaguecommunities.adobe.com/t5/user/viewprofilepage/user-id/17916475)

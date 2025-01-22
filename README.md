<div align="center">
  <img align="center" src="./img/logo.png" width="100" height="100" />
</div>

<h2 align="center">SuperHotSwap <sup></sup></h2>
<h4 align="center"><strong>English</strong> | <a href="./README_CN.md">简体中文</a></h4>

![Java](https://img.shields.io/badge/Java-ED8B00.svg?logo=java&logoColor=white)
![IntelliJ](https://img.shields.io/badge/IntelliJ%20IDEA-black?logo=intellij-idea&logoColor=white)
[![License](https://img.shields.io/github/license/songbiaoself/SuperHotSwap?color=blue)](./LICENSE)
[![downloads](https://img.shields.io/jetbrains/plugin/d/24290)](https://plugins.jetbrains.com/plugin/24290-superhotswap)
[![release](https://img.shields.io/jetbrains/plugin/v/24290?label=version)](https://plugins.jetbrains.com/plugin/24290-superhotswap)
![sdk](https://img.shields.io/badge/plugin%20sdk-IDEA%202021.3-red.svg)

The original intention of development: The aim is to make the most convenient IDEA hot update plug-in, reduce user operation steps, and provide visual operation updates with zero configuration.

## Development environment

- JDK1.8
- IDEA2021.3
- Gradle8.7

## Support features

| Support Functions | | Description |
|---------------|------|---------------------------------------------------------------------|
| MybatisXML Hot Update | √    | Supports hot updates of content such as select/insert/delete/update/resultMap/parameterMap/sql/cache
| Class Hot Update | √    | Warm updates can be modified within methods natively. Enhancements such as modifying field method classes and hot update of lombok annotations require the installation of the dcevm patch, as shown in the following tutorial
| Spring Hot Update | √    | Bean registration and destruction are supported. RequestMapping can be dynamically updated
| Remote Hot Update | Ongoing |                                                                     |
| ...           | ...  |                                                                     |

## Use the process

1. Search for installs in the plugin marketplace
![img.png](img/install.png)

2. Start the project

After the installation is successful, restart IDEA, and output a banner after starting the project to indicate that the installation is successful
![img.png](img/banner.png)

3. Mapper hot update

Click the 'File Hot Swap' button in the MapperXML file to execute the hot update command, and the normal output of the command is as follows:
![img.png](img/xml-hotswap.png)

4. Java hot update

![img.png](img/class-hotswap.gif)

5. Spring hot update

APIs can be created, edited, and deleted dynamically. The demonstration is as follows.
![img.png](img/spring-hotswap.gif)

6. jar file hot updates

Supports hot update of source code files in .class files and jar packages.
![img.png](img/jar-hotswap.png)

## Install hot update patches
Limitations of hot-like updates

- The parent class of the new class and the old class must be the same.
- The new class and the old class should also implement the same number of interfaces, and they should be the same interfaces.
- The new class and the old class accessors must be the same.
- The number of fields and field names of the new and old classes must be the same.
- Methods added or deleted from new and old classes must be private static/final.
- You can modify the method body.

If you want to remove the restrictions, you need to install the jdk patch, and the DECVM patch can be downloaded at:
[https://github.com/dcevm/dcevm](https://github.com/dcevm/dcevm)

Download the patch of the corresponding JDK version and replace it to complete the installation.

![img.png](img/jvmdll.png)

Output version, successful output example

![img.png](img/dll-example.png)

## Contact

Support is available for a fee, contact details are below.

Gitee Address: https://gitee.com/song_biao/super-hot-swap

github address: https://github.com/songbiaoself/SuperHotSwap

Email 📫: <646997146@qq.com>

Official account: CodeRevolt

# IdGuard

[![](https://jitpack.io/v/ZTWave/IdGurad.svg)](https://jitpack.io/#ZTWave/IdGurad)

#### 介绍
Android Layout xml name and id obfuscate gradle plugin.

#### 使用说明

 **注：请在执行操作前备份代码** 

在project的build.gradle中的buildscript下添加

```
repositories {  
  maven { url 'https://jitpack.io' }  
}  
dependencies {  
  classpath 'com.github.ZTWave:IdGurad:v0.0.4'  
}
```

在app下的build.gradle中添加

```
plugins {
  id 'idguard'
}
```

![GradleTask](https://foruda.gitee.com/images/1688439704923784844/c9fdf530_1636113.png "屏幕截图")

#### 感谢

[XmlClassGuard](http://https://github.com/liujingxing/XmlClassGuard)

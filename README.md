# ZoomableImageView

This is a custom component for Android to zoom images using touch gestures.

## Features

- Fluid enlargement and reduction of images.
- Image scrolling with touch gestures.

## Requeriments

- API 24 or more.

## Setup dependencies

### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.????</'
}
```

### Maven

```xml
<!-- <repositories> section of pom.xml -->
<repository>
    <id>jitpack.io</id>
   <url>https://jitpack.io</url>
</repository>

<!-- <dependencies> section of pom.xml -->
<dependency>
    <groupId>com.github.dgqstudio</groupId>
    <artifactId>????</artifactId>
    <version>????</version>
</dependency>
```

## Usage

```xml
<com.dgqstudio.imagezoom.ZoomableImageView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:srcCompat="@drawable/your_image" />
```
/*
 * Copyright (C) 2020 Robert Stupp, All rights reserved.
 * snazy@snazy.de
 */

plugins { `kotlin-dsl` }

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies { implementation(gradleKotlinDsl()) }

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlinDslPluginOptions { jvmTarget.set(JavaVersion.VERSION_11.toString()) }

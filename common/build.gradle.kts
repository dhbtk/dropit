import dropitconf.Deps

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("dependencies-plugin")
}

description = ""

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation(Deps.junitJupiterEngine)

    api(Deps.dagger)
    api(Deps.jacksonDatabind)
    api(Deps.jacksonModuleKotlin)
    api(Deps.retrofit)
    api(Deps.retrofitConverterJackson)
    api(Deps.retrofitConverterScalars)
    api(Deps.okHttp)
    api(Deps.okHttpLoggingInterceptor)
    api(Deps.rxJava)

    kapt(Deps.daggerCompiler)
}

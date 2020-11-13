plugins {
    use(Deps.Plugins.kotlinJvm)
    use(Deps.Plugins.kotlinKapt)
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

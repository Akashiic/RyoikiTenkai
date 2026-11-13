plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.test {
    useJUnitPlatform()
}

// O harness de desenvolvimento (com.akashic.ryoiki.dev) nunca vai no jar publicado: só entra com -Pharness.
//   echo warm > run/client/ryoiki-harness && ./gradlew runClient -Pharness
val withHarness = providers.gradleProperty("harness").isPresent
tasks.named<Jar>("jar") {
    if (!withHarness) {
        exclude("com/akashic/ryoiki/dev/**")
    }
}
tasks.withType<JavaExec>().configureEach {
    if (withHarness) {
        jvmArgs("-Dryoiki.devHarness=true")
    }
}

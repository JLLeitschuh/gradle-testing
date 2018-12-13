

// Keep this at the bottom of this file.
tasks.withType<Wrapper>().configureEach {
    gradleVersion = "5.0"
    distributionType = Wrapper.DistributionType.ALL
}

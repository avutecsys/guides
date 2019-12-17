// tag::container-api[]
tasks.register("greeting") {
    doLast { println("Hello, World!") }
}

// end::container-api[]

// tag::typed-container-api[]
tasks.register<Zip>("docZip") {
    archiveName = "doc.zip"
    from("doc")
}
// end::typed-container-api[]

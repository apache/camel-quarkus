[camel-quarkus-[=kindPural]]
= Camel [=humanReadableKindPlural] supported on Quarkus

[=components?size] [=humanReadableKindPlural] in [=numberOfArtifacts] JAR artifacts ([=numberOfDeprecated] deprecated[#if numberofJvmOnly > 0], [=numberofJvmOnly] JVM only[/#if])

[width="100%",cols="4,1,1,1,5",options="header"]
|===
| [=humanReadableKind?cap_first] | Artifact | Support Level | Since | Description
[#list components as row]

| [#if getDocLink(row)??][=getDocLink(row)][[=row.title]][#else]([=row.title])[/#if] | [=row.artifactId] | [=getTarget(row)] +
[=getSupportLevel(row)] | [=row.firstVersion] | [#if row.deprecated]*deprecated* [/#if][=row.description]
[/#list]
|===

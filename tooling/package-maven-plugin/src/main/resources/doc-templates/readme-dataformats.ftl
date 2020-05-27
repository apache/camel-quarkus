Number of Camel data formats: [=components?size] in [=numberOfArtifacts] JAR artifacts ([=numberOfDeprecated] deprecated)

[width="100%",cols="4,1,1,1,5",options="header"]
|===
| Data Format | Artifact | Support Level | Since | Description
[#list components as row]

| [=getDocLink(row)][[=row.title]] | [=row.artifactId] | [=getTarget(row)] + [=getSupportLevel(row)] | [=row.firstVersion] | [#if row.deprecated]*deprecated* [/#if][=row.description]
[/#list]
|===

Number of Camel components: [=components?size] in [=numberOfArtifacts] JAR artifacts ([=numberOfDeprecated] deprecated)

[width="100%",cols="4,1,1,5",options="header"]
|===
| Component | Target +
Level | Since | Description
[#list components as row]

| [=getDocLink(row)][[=row.title]] ([=row.artifactId]) +
`[=row.syntax]` | [=row.target] +
 [=row.supportLevel] | [=row.firstVersion] | [#if row.deprecated]*deprecated* [/#if][=row.description]
[/#list]

|===

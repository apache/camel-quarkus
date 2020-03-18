Number of miscellaneous extensions: [=components?size] in [=numberOfArtifacts] JAR artifacts ([=numberOfDeprecated] deprecated)

[width="100%",cols="4,1,1,5",options="header"]
|===
| Extension | Target Level | Since | Description
[#list components as row]

|[#if getDocLink(row)??] [=getDocLink(row)][[=row.artifactId]] [#else] ([=row.artifactId])[/#if] | [=row.target] +
 [=row.supportLevel] | [=row.firstVersion] | [#if row.deprecated]*deprecated* [/#if][=row.description]
[/#list]
|===

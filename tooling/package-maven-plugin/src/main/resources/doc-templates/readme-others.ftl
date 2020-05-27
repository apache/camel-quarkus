Number of miscellaneous extensions: [=components?size] in [=numberOfArtifacts] JAR artifacts ([=numberOfDeprecated] deprecated)

[width="100%",cols="4,1,1,1,5",options="header"]
|===
| Extension | Artifact | Support Level | Since | Description
[#list components as row]

| [#if getDocLink(row)??] [=getDocLink(row)][[=row.title]] [#else] ([=row.title])[/#if] | [=row.artifactId] | [=getTarget(row)] + [=getSupportLevel(row)] | [=row.firstVersion] | [#if row.deprecated]*deprecated* [/#if][=row.description]
[/#list]
|===

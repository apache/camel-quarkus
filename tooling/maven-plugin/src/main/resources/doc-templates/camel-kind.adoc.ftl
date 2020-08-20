[camel-quarkus-[=kindPural]]
= Camel [=humanReadableKindPlural] supported on Quarkus

[=r"[#"]cq-[=kindPural]-table-row-count]##?## [=humanReadableKindPlural] in [=r"[#"]cq-[=kindPural]-table-artifact-count]##?## JAR artifacts ([=r"[#"]cq-[=kindPural]-table-deprecated-count]##?## deprecated, [=r"[#"]cq-[=kindPural]-table-jvm-count]##?## JVM only)

[=r"[#"]cq-[=kindPural]-table.counted-table,width="100%",cols="4,1,1,1,5",options="header"]
|===
| [=humanReadableKind?cap_first] | Artifact | Support Level | Since | Description
[#list components as row]

| [#if getDocLink(row)??][=getDocLink(row)][[=row.title]][#else]([=row.title])[/#if] | [.camel-element-artifact]##[=row.artifactId]## | [.camel-element-[=getTarget(row)]]##[=getTarget(row)]## +
[=getSupportLevel(row)] | [=row.firstVersion] | [#if row.deprecated][.camel-element-deprecated]*deprecated* [/#if][=row.description]
[/#list]
|===

++++
<script type="text/javascript">
var countedTables = document.getElementsByClassName("counted-table");
if (countedTables) {
    var i;
    for (i = 0; i < countedTables.length; i++) {
        var table = countedTables[i];
        var tbody = table.getElementsByTagName("tbody")[0];
        var rowCountElement = document.getElementById(table.id + "-row-count");
        rowCountElement.innerHTML = tbody.getElementsByTagName("tr").length;
        var deprecatedCountElement = document.getElementById(table.id + "-deprecated-count");
        deprecatedCountElement.innerHTML = tbody.getElementsByClassName("camel-element-deprecated").length;
        var jvmCountElement = document.getElementById(table.id + "-jvm-count");
        jvmCountElement.innerHTML = tbody.getElementsByClassName("camel-element-JVM").length;

        var artifactCountElement = document.getElementById(table.id + "-artifact-count");
        var artifactElements = tbody.getElementsByClassName("camel-element-artifact");
        var artifactIdSet = new Set();
        var j;
        for (j = 0; j < artifactElements.length; j++) {
            artifactIdSet.add(artifactElements[j].innerHTML);
        }
        artifactCountElement.innerHTML = artifactIdSet.size;
    }
}
</script>
++++

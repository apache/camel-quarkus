[camel-quarkus-extensions]
= Camel Quarkus extensions reference
:page-aliases: list-of-camel-quarkus-extensions.adoc,reference/extensions/index.adoc

[TIP]
====
In case you are missing some extension in the list:

* Upvote https://github.com/apache/camel-quarkus/issues[an existing issue] or create
  https://github.com/apache/camel-quarkus/issues/new[a new one] so that we can better prioritize our work.
* You may also want to try to add the extension yourself following our xref:contributor-guide/index.adoc[Contributor guide].
* You may try your luck using the given camel component on Quarkus directly (without an extension). Most probably it
  will work in the JVM mode and fail in the native mode. Do not hesitate to
  https://github.com/apache/camel-quarkus/issues[report] any issues you encounter.
====

[=r"[#"]cq-extensions-table-row-count]##?## extensions ([=r"[#"]cq-extensions-table-deprecated-count]##?## deprecated, [=r"[#"]cq-extensions-table-jvm-count]##?## JVM only)

[=r"[#"]cq-extensions-table.counted-table,width="100%",cols="4,1,1,1,5",options="header"]
|===
| Extension | Artifact | Support Level | Since | Description
[#list components as row]

| [#if getDocLink(row)??] [=getDocLink(row)][[=row.title]] [#else] ([=row.title])[/#if] | [=row.artifactId] | [.camel-element-[=getTarget(row)]]##[=getTarget(row)]## +
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
    }
}
</script>
++++

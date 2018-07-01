def escapePackageName(str){
    if(str == null) return "";
    def trimmed = str.trim();
    if (trimmed.startsWith("/")) trimmed = trimmed - "/";
    return trimmed.replaceAll("-", "_").replaceAll("/", ".");
}
def props = [:]

props.path = ask("What path will clientlib-async be under? [${projectDir}]", "/apps/clientlib-async", "path")
props.path = "${props.path}/${projectDir.name}"
props.packageName = "${escapePackageName(props.path)}"

processTemplates "clientlib.html", props
processTemplates "ClientLibUseObject.java", props
processTemplates "graniteClientLib.html", props
processTemplates "README.md", props
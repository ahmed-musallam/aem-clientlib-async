# remobe default async-clientlib then re-create it from local template
rm -rf async-clientlib && lazybones create aem-clientlib-async 1.0.0 async-clientlib  -Ppath=/apps

# aem-clientlib-async-template
A lazy bones template to create your own `aem-clientlib-async` template.
Read more about aem-clientlib-async on the project main README.md.

## Developing
### Scripts
  1. `install-template.sh` installs template to local lazybones cache
  2. `publish-template` publishes the template to my bintray account
  
### Developer workflow
  1. Make changes to template under `templates/aem-clientlib-async`
  2. Run `install-template.sh` to install template locally tolazybones cache
  3. To test the remplate, in a different directory, run `lazybones create aem-clientlib-async <version> <project-name>  -Ppath=<path-to-use>`
      - an exampele test command `lazybones create aem-clientlib-async 1.0.0 async-clientlib  -Ppath=/apps/my-project`

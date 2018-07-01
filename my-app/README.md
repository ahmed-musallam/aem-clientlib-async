# aem-clientlib-async

> This project was generated using the [aem-async-clientlib lazebones generator](https://github.com/ahmed-musallam/aem-async-clientlib)

A simple template to add support for `async`, `defer`, `onload` and `crossorigin` attributes for clientlibrary HTML. The same as Nate Yolles [aem-clientlib-async)](https://github.com/nateyolles/aem-clientlib-async) only with a slightly different implementation, read more about in the java use class;

## How to use in your project:

1. The current folder bust be at path: /apps/my-app in your AEM project 
2. Use this as you would the defalt clienlib template, examples:
	
	```HTML
    <head data-sly-use.clientLib="${'/apps/my-app/clientlib.html'}">
	
    <!--/* for css+js */-->
    <meta data-sly-call="${clientLib.all @ categories='your.clientlib'}" data-sly-unwrap></meta>
	
    <!--/* only js */-->
    <meta data-sly-call="${clientLib.js @ categories='your.clientlib'}" data-sly-unwrap></meta>
	
    <!--/* only css */-->
    <meta data-sly-call="${clientLib.css @ categories='your.clientlib'}" data-sly-unwrap></meta>
	
    </head>
	```
3. Using `async`, `defer`, `onload` and `crossorigin`

	```
    <!--/* async */-->
    <meta data-sly-call="${clientLib.js @ categories='your.clientlib', loading='async'}" data-sly-unwrap></meta>
	
    <!--/* defer */-->
    <meta data-sly-call="${clientLib.js @ categories='your.clientlib', loading='defer'}" data-sly-unwrap></meta>
	
    <!--/* defer and onload */-->
    <meta data-sly-call="${clientLib.js @ categories='your.clientlib', loading='defer', onload='myFunction()'}" data-sly-unwrap></meta>
	
    <!--/* crossorigin */-->
    <meta data-sly-call="${clientLib.all @ categories='your.clientlib', crossorigin='anonymous'}" data-sly-unwrap></meta>
	```
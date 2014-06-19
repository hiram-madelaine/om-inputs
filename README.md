# The goal of this library is to rapidly prototype UI with Om/React

The library generates responsive components based on a description of the data.

The library uses [Prismatic/Schema](https://github.com/Prismatic/schema) to describe the data.

Using Schema allows the :
* Validation of the data ;
* Coercion of String to types.

## How does it work

### Anatomy of a component


To build a component we need :
* A name ;
* A description of the fields ;
* A callback function to use the data ;
* Options to customize the component.



#### The component name

The name is used :

* as the React display name;
* To differentiate components in the UI.


#### Description of the fields

The fields of a component are described with Schema :

```
(def form {:person/name s/Str
           :person/size s/Int
           :person/gender (s/enum "M" "Ms")})
```

### Translation of the Schema into UI.


Each entry of a schema generate a field in the form.
The example schema will produce a form with three input fields :

* An input of type text for :person/name
* An input that allows only Integer for :person/size
* A select that that present the choices "M" and "Ms"




#### Options


#### i18n

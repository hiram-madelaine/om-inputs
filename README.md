# The goal of this library is to rapidly prototype UI with Om/React

The library generates responsive components based on data description.

The library uses [Prismatic/Schema](https://github.com/Prismatic/schema) to describe the data.

Using Schema allows the :
* Validation of the data ;
* Coercion of String to proper types.

## How does it work

### Anatomy of a component


To build a component we need :
* A name ;
* A description of the fields ;
* A callback function to use the data ;
* Options to customize the component.



#### The component name

The name is used :

* as the React.js display name;
* To differentiate components in the UI.


#### Description of the fields

The fields of a component are described with Schema :

```
(def sch-person {:person/first-name s/Str
                 :person/name s/Str
                 (s/optional-key :person/size) s/Int
                 (s/optional-key :person/gender) (s/enum "M" "Ms")})
```

#### The calback function

The callback function takes the cursor app state, the owner and the entity.

`(fn [app owner entity])`


### Build an Om input component

To build an Om input component, just call the function `make-input-comp` with the required parameters :
- A keyword for the component name
- A Prismatic/Schema
- a callback function

In this example we build the component :create-person with the Schema seen previously and the callback simply diplay the created map :

```
(def person-input-view (make-input-comp :create-person sch-person #(js/alert %3)))
```


### Translation of the Schema into UI.


#### The form inputs

Each entry of a schema generate a field in the form.
The example schema will produce a form with these input fields :

* A mandatory input of type text for :person/first-name ;
* A mandatory input of type text for :person/name ;
* An optional input that allows only Integer for :person/size ;
* An optional select that that present the choices "M" and "Ms" ;
* A validation button that trigger the callback.

#### The validation


When clicking the action button, the form is validated according to the Schema :

* A required input must have a non blank value ;
* A coercion appends if needed for type different than s/Str



#### Options


#### i18n

It is possible to provide the labels in multiple languages.
Just put a map in the shared data :
```
(om/root
 person-input-view
 {:lang "fr"}
 {:target (. js/document (getElementById "person"))
  :shared {:i18n {"fr" {:create-person {:action "Créer personne"
                                       :person/name {:label "Nom"}
                                       :person/first-name {:label "Prénom"}
                                       :person/size {:label "Taille"}
                                       :person/gender {:label "Genre"}}}}}})
```


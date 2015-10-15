# A ClojureScript library that generates Om/React forms

> You just go down the list of all the most important things on the internet and what you find is :    
> a Form is usually the broker for the stuff that matters  

[Luke Wroblewski from Conversions@Google 2014 - Part 2](https://www.youtube.com/watch?v=nmKMz3Fg76M)  


## Project Goals

* Generate forms with specification using [Prismatic/Schema](https://github.com/Prismatic/schema).
* Provide best practice UX
* Use no external JS Framework dependencies beside React.js


## Artifacts

[![Clojars Project](http://clojars.org/om-inputs/latest-version.svg)](http://clojars.org/om-inputs)

## Project Maturity

om-inputs is used for real production projects that are used every day.
It has been tested by QA Team.
At this stage, the project will minimize the breaking changes as it could impact production code.


## Show me what it looks like and how to use it !

[You can try it online with the interactive tutorial](http://hiram-madelaine.github.io/om-inputs/playground)


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

The fields of a component are described with Schema.

Using Schema allows the :
* Validation of the data ;
* Coercion of String to proper types.

##### Supported atomic Schema types

* s/Str
* s/Int
* s/Inst
* s/enum
* s/Bool
* s/Eq
* s/Regex

A value can be nil using s/maybe :

```clj
{:person/first-name (s/maybe s/Str)}

```


A key can be optional using s/s/optional-key :
```clj
 {(s/optional-key :person/size) s/Num}
```

##### Example

```clj
(def sch-person {:person/first-name s/Str
                 :person/name s/Str
                 (s/optional-key :person/birthdate) s/Inst
                 (s/optional-key :person/size) s/Int
                 (s/optional-key :person/gender) (s/enum "M" "Ms")})
```

#### The callback function

The callback function takes the cursor app state, the owner and the entity.

```clj
(fn [app owner entity])
```

##### Callback options

```clj
{(s/optional-key :action) {(s/optional-key :one-shot) s/Bool
                             (s/optional-key :no-reset) s/Bool
                             (s/optional-key :async) s/Bool
                             (s/optional-key :attrs) {s/Any s/Any}}}
```

##### Asynchronous action

```clj
{:action {:async true}}
```

An action can also be asynchronous. In this case the callback fn must have 4 args :
`(fn [app owner entity chan])`
The fourth argument is a core.async channel in which the fn puts the succes or failure of the operation.
* :ok for success ;
* :ko in case of failure.


### Build an Om input component

To build an Om input component, just call the function `make-input-comp` with the required parameters :
- A keyword for the component name
- A Prismatic/Schema
- a callback function

In this example we build the component :create-person with the Schema seen previously and the callback simply diplay the created map :

```clj
(def person-input-view (make-input-comp :create-person sch-person #(js/alert %3)))
```


### Translation of the Schema into UI.



#### The form inputs

Each entry of a schema generate a field in the form.

Hence, the example schema will produce a form with these input fields :

* A mandatory input of type text for :person/first-name ;
* A mandatory input of type text for :person/name ;
* An optional date input for the :person/birthdate ;
* An optional input that allows only Integer for :person/size ;
* An optional select that that present the choices "M" and "Ms" ;
* A validation button that trigger the callback.

#### Fields validation

There are two type of validations :

1.  Schema validation
2.  Verily validation

##### Schema Validation

Schema is able to check that a data structure is conform :

In case of a map :

* all required keys are present ;
* All values are of the correct type ;

This job is done by Schema/Coercion :
When a value is not of the declared type, we have a chance to coerce it in the correct type.


The problem with an HTML form is that all data are strings.

* An empty string represents nil
* Other types must be coerced to the correct type : s/Num, s/Int, s/Inst



##### When validations occur ?

###### Inline validation

Each field is validated when leaving the input.

If the field is required and left empty the field is maked invalid and a tooltip is displayed.




###### Submission validation

When clicking the action button, the form is validated according to the Schema :

* A required input must have a non blank value ;
* A coercion appends if needed for type different than s/Str


#### Options

Options are a mean to override the default behavior of the library.

All options are given in a map.


##### Order of fields

The schema is a map that can't be ordered so the fields are displayed in a random order.


You can define the total ordering by giving a vector :

```clj
(def opts {:order [:person/first-name :person/name :person/gender :person/birthdate :person/size :person/married]})
```

##### Change the rendering (implementation may change)


###### Different way to display an enum

By default the enum is display as a dropdown list
It is possible to choose different representations :
Vertical Group of radio buttons :
```clj
(def opts {:person/gender {:type "radio-group"}})
```
Horizontal group of radio buttons :

```clj
(def opts {:person/gender {:type "radio-group-inline"}})
```

Segmented controls :
```clj
(def opts {:person/gender {:type "btn-group"}})
```

Stepper (-|+) :
```clj
(def opts {:items/number {:type "stepper"
                          :attrs {:min 0 :max 10 :step 1}}})
```


##### More Complex Validation rules

It is possible to add more complex validation rules than the one provided by Schema.

I chose [Verily](https://github.com/jkk/verily) for the following reasons :

* the rules can be described as data structure
* the rules are expressed on the whole map not by key.
* It works for Clojure and ClojureScript.


My goal is to be able to plug any other validation framework.


######  Add validations rules


```clj
(def opts {:validations [[:min-val 100 :person/size :person-size-min-length]
                         [:email :person/email :bad-email]]})
```



##### Initial value (not implemented yet)

It should be possible to have initial values for each field.

```clj
(def opts {:init {:person/married true}})
```

The initial data could be retrieved from the cursor app-state.


#### i18n

It is possible to provide the labels and error messages in multiple languages.
Just put a map in the shared data :

```clj
(om/root
 app-view
 app-state
 {:target (. js/document (getElementById "person"))
  :shared {:i18n {"en" {:language {:action "Change language"
                                   :lang {:label "Language"
                                          :data {"en" "English"
                                                 "fr" "French"}}}
                        :create-person {:action "Create person"
                                        :person/name {:label "Name"}
                                        :person/birthdate {:label "Birthday"}
                                        :person/first-name {:label "Firstname"}
                                        :person/size {:label "Size"}
                                        :person/gender {:label "Gender"
                                                        :data {"M" "Mister"
                                                               "Ms" "Miss"}}}}
                  "fr" {:language {:action "Choix de la langue"
                                   :lang {:label "Langue"
                                          :data {"en" "Anglais"
                                                 "fr" "Français"}}}
                        :create-person {:action "Créer personne"
                                       :person/name {:label "Nom"}
                                       :person/first-name {:label "Prénom"}
                                       :person/birthdate {:label "Date de naissance"}
                                       :person/size {:label "Taille"}
                                       :person/gender {:label "Genre"
                                                       :data {"M" "Monsieur"
                                                              "Ms" "Madame"}}}}}}})

```

## Copyright and license

Copyright © 2013-2015 Hiram Madelaine

Licensed under the EPL (see the file LICENSE.md).

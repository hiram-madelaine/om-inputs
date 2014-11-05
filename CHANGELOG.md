## 0.3.6-SNAPSHOT

* ADD Layout indirection for input
* ADD New input display segmented control with : btn-group and radio-btn-group
* TEC Rewrite the way inputs are build. Each input is now given only its data.


## 0.3.5-SNAPSHOT

* ADD Experimental handling of asynchroneous action. New options {:action {:async true}}. The callback accepts a fourth argument that is the channel. The callback must put! :ok or :ko at the end.
* ADD the possibility to inline radio-group with the new type "radio-group-inline"
* FIX Position of toolip messages
* ADD Tooltip for info. In i18N add :info to a field.

## 0.3.4-SNAPSHOT

* ADD Experimental "now" to capture real time occurence for s/inst
* ADD Possibility to alter opts dynamically via :dyn-opts in root local state. Merge with init opts during render phase.
* ADD Possibility to add hooks to lifecycle methods. First is IWillReceiveProps. Add :IWillReceiveProps and (fn [owner next-props]) to the options map.
## 0.3.3-SNAPSHOT

* ADD flag no-reset to keep last values instead of initial values

## 0.3.2-SNAPSHOT

* FIX Complete validation on action bouton is now playing all unit validations
* ADD id for action and clean button comp-name-[action|clean]
* FIX clean function was not called.


## 0.3.1-SNAPSHOT

* EVOL Add 1-1 mode for validation.

## 0.2.9-SNAPSHOT

* FIX Labeled inputs.

## 0.2.8-SNAPSHOT

* EVOL Add placeholder support. `{:ph "blah blah"}`
* FIX Reset of input fields was broken.

## 0.2.7-SNAPSHOT

* FIX typing control was broken on first use.

## 0.2.6-SNAPSHOT

* EVOL support for Schema s/Eq : A proper error message is still missing.
* EVOL support for Schema s/Regex : It controls the characters one can type in the input field.


## 0.2.5-SNAPSHOT

* EVOL Better support for initial values of fields

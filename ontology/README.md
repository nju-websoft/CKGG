# Source for ontology

## Structure

- `ontology.yml`: The manually constructed CKGG ontology using a DSL based on YAML
- `process_ontology.py`: Compiles ontology to OWL file
- `toc.csv`: Title of chapters used to track the provenance of terms

## Buliding ontology from yaml source

```
python process_ontology.py --ignore_explicit_instance --use_string_for_enum
```

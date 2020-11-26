# markup

![Java CI with Maven](https://github.com/erniehs/markup/workflows/Java%20CI%20with%20Maven/badge.svg)

a very simple markup "engine".

## some spec

- as a markup engine i should,
  - take any correctly structured json as an input
  - take correctly structured json path as markup trigger
  - take correctly structured json as the markup to apply
  - return correctly structured json as markup results
    - the original json supplied
    - the markup "rules" fired
    - the markup rules evaluation

for example,

```json
{
  "customers": [
    {
      "customer": "john",
      "cheese": "chedder",
      "account": {
        "total": 100.45
      }
    },
    {
      "customer": "jane",
      "account": {
        "total": 50.56
      }
    }
  ]
}
```

with markup rules,

```json
{
  "rule": "cheese markup, tax those who love cheese...!",
  "pattern": "$..customers[?(@.cheese)]",
  "markup": {
    "pattern": "$.account.total",
    "add": 5.23
  }
}
```

will find this,

```json
[
  {
    "customer": "john",
    "cheese": "chedder",
    "account": {
      "total": 100.45
    }
  }
]
```

and apply the markup giving a result of,

```json
{
  "data": {
    "customers": [
      {
        "customer": "john",
        "cheese": "chedder",
        "account": {
          "total": 100.45
        }
      },
      {
        "customer": "jane",
        "account": {
          "total": 50.56
        }
      }
    ]
  },
  "fired": [{ "rule": "cheese markup, tax those who love cheese...!" }],
  "result": [
    {
      "customer": "john",
      "cheese": "chedder",
      "account": {
        "total": 105.77
      }
    }
  ]
}
```

## considerations

- order of rules,
- do rules accumulate, or apply independently of each other...?
- rule partitioning, if then else whatever who cares and so on...
- for example, add 5 then add 5%
  - is it (total + 5) + (total + 5%)
  - is it (total + 5) \* 5%

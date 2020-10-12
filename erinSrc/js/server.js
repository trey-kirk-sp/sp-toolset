const jsonServer = require('json-server');
const server = jsonServer.create();
const router = jsonServer.router('./db.json');
const middlewares = jsonServer.defaults();
const port = process.env.PORT || 3000;
var db = require('./db.json');

var baseListResult = {
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
    "startIndex": 1,
    "totalResults": 0,
    "Resources": []
};
console.log ("Lez do this");
server.use (jsonServer.rewriter ({
    '/Schemas/*': '/Schemas?filter=id+eq+%22$1%22',
    '/Users/*': '/Users?filter=id+eq+%22$1%22',
    '/Containers/*': '/Containers?filter=id+eq+%22$1%22',
    '/PrivilegedData/*': '/PrivilegedData?filter=id+eq+%22$1%22',
    '/Groups/*': '/Groups?filter=id+eq+%22$1%22',
    '/ResourceTypes/*': '/ResourceTypes?filter=name+eq+%22$1%22',
    '/ContainerPermissions/*': '/ContainerPermissions?filter=id+eq+%22$1%22',
    '/PrivilegedDataPermissions/*': '/PrivilgedDataPermissions?filter=id+eq+%22$1%22'
}));
server.use(middlewares);

function BaseListResult (resources) {
    this.schemas = ["urn:ietf:params:scim:api:messages:2.0:ListResponse"];
    this.startIndex = 1;
    if (resources != null) {
        this.totalResults = resources.length;
    } else {
        this.totalResults = 0;
    }
    this.Resources = resources;
};

// Store the DB as a big array of 'resources'. use their Schema values to populate various paths

// Return a ListResult of objects per resourceType and optional query
function getObjectsByLocation (resourceType, query) {
    console.log ("Searching for location resourceType: " + resourceType);
    let results = [];
    db.Resources.forEach (resource => {
        // 1.0 schemas don't have meta
        if (resource.meta && resource.meta.resourceType === resourceType) {
            results.push (resource);
        }
    });

    return results;
};

function getObjectsBySchema(schema, query) {
    console.log ("Searching for schema: " + schema);
    // Return an array of objects per schema and optional query
    // schema is a token of the full schema, so have to do tokenized search
    // lazy dev says just do an 'endsWith' and assume our token has the
    // : delimter
    let results = [];
    db.Resources.forEach (resource => {
        schemas = resource.schemas;
        // Some 2.0 objects, like Schema:EnterpriseUser, might not have schemas
        if (schemas) {
            for (i = 0; i < schemas.length; i++) {
                if (schemas[i].endsWith(schema)) {
                    results.push (resource);
                }
            }
        }
    });

    return results;
};

/*
 * Use the token to find objects by schema and by meta location
 */
function getObjects (token, query) {
    console.log ("query: " + JSON.stringify (query));
    let bySchemaObjects = getObjectsBySchema (":" + token);
    let byLocationObjects = getObjectsByLocation (token);
    // Use a Set to eliminate duplicates
    let resultSet = new Set(bySchemaObjects.concat (byLocationObjects));
    // Convert the Set into an array
    let results = [...resultSet];
    // Filter the results
    results = filter (results, query);
    if (results.length > 1) {
        let listResult = new BaseListResult (results);
        return listResult;
    } else if (results.length === 1) {
        return results[0];
    } else {
        // Return a ListResult w/ no results
        return new BaseListResult();
    }
}

function filter (results, query) {
    // Given set of results and a query, eliminate values not matching the query
    // right now I only understand 'something eq something'
    let filter = query ['filter'];
    let filteredResults = results;
    if (filter != null) {
        filteredResults = [];
        let tokens = filter.split(" ");
        if (tokens != null && tokens.length === 3) {
            // test if tokens length is 3
            console.log ("tokens: " + tokens);
            let property = tokens[0];
            let op = tokens[1];
            let value = tokens[2];
            results.forEach (resource => {
               if (op === "eq") {
                    let propValue = readPath (resource, property);
                    if ('\"' + propValue + '\"' === value) {
                        filteredResults.push (resource);
                    }
               } 
            });
        }
    }
    return filteredResults;
};

function readPath (object, property) {
    // console.log ("Reading " + JSON.stringify (object));
    // console.log ("For property: " + property);
    // Given a property or deep property, return the relevant value
    let value = null;
    let tokens = property.split (".", 2);
    if (tokens.length === 1) {
        // sep, just do the deed
        return object[property];
    } else {
        // Use the first element to extract the nested object and recurse into for the sub property in the second element
        let nestedObj = object[tokens[0]];
        return readPath (nestedObj, tokens[1]);
    }
}

server.get ('/Users', (req, res) => {
    let results = getObjects ('User', req.query);
    res.status(200).jsonp(results);
});

server.get ('/Containers', (req, res) => {
    let results = getObjects ('Container', req.query);
    res.status(200).jsonp(results);
});

server.get ('/PrivilegedData', (req, res) => {
    let results = getObjects ('PrivilegedData', req.query);
    res.status(200).jsonp(results);
});

server.get ('/Groups', (req, res) => {
    let results = getObjects ('Group', req.query);
    res.status(200).jsonp(results);    
});

server.get ('/ResourceTypes', (req, res) => {
    let results = getObjects ('ResourceType', req.query);
    res.status(200).jsonp(results);
});

server.get ('/Schemas', (req,res) => {
    let results = getObjects ('Schema', req.query);
    res.status(200).jsonp(results);
});

server.get ('/ContainerPermissions', (req, res) => {
    let results = getObjects ('ContainerPermission', req.query);
    res.status(200).jsonp(results);
});

server.get ('/PrivilegedDataPermissions', (req, res) => {
    let results = getObjects ('PrivilegedDataPermission', req.query);
    res.status(200).jsonp(results);
});

server.use(router);
server.listen(port);


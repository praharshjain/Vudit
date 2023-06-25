(function () {
    var root = this;
    var $file_input = document.querySelector("input#file");
    var $holder = document.querySelector("#notebook-holder");

    var render_notebook = function (ipynb) {
        var notebook = root.notebook = nb.parse(ipynb);
        while ($holder.hasChildNodes()) {
            $holder.removeChild($holder.lastChild);
        }
        $holder.appendChild(notebook.render());
        Prism.highlightAll();
    };

    var load_file = function (file) {
        var reader = new FileReader();
        reader.onload = function (e) {
            var parsed = JSON.parse(this.result);
            render_notebook(parsed);
        };
        reader.readAsText(file);
    };

    $file_input.onchange = function (e) {
        load_file(this.files[0]);
    };

    window.addEventListener('dragover', function (e) {
        e.stopPropagation();
        e.preventDefault();
        e.dataTransfer.dropEffect = 'copy';
        root.document.body.style.opacity = 0.5;
    }, false);

    window.addEventListener('dragleave', function (e) {
        root.document.body.style.opacity = 1;
    }, false);

    window.addEventListener('drop', function (e) {
        e.stopPropagation();
        e.preventDefault();
        load_file(e.dataTransfer.files[0]);
        $file_input.style.display = "none";
        root.document.body.style.opacity = 1;
    }, false);

    function getParameterByName(name, url) {
        if (!url) {
            url = window.location.href;
        }
        name = name.replace(/[\[\]]/g, "\\$&");
        let regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, " "));
    }

    function readFile(filePath) {
        let rawFile = new XMLHttpRequest();
        rawFile.open("GET", filePath, false);
        rawFile.onreadystatechange = function () {
            if (rawFile.readyState === 4) {
                if (rawFile.status === 200 || rawFile.status == 0) {
                    render_notebook(JSON.parse(rawFile.responseText));
                }
            }
        }
        rawFile.send(null);
    }

    const filePath = getParameterByName('file');
    readFile(filePath);
}).call(this);

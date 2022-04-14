/**
 * Request Host app to mark paragraph as read
 */
function hostAppParseParagraph(paragraphText) {
  var split = '?paragraphText=';
  window.open(split + encodeURI(paragraphText), '_self');
}

function createButton(text) {
  var button = document.createElement("div");
  button.className = "pd-button"
  button.innerText = text
  return button
}

function createControlsContainer() {
  var div = document.createElement("div")
  div.className = "pd-controls"
  return div
}

function injectParagraph() {
  var paragraphs = document.querySelectorAll("p")

  paragraphs.forEach(p => {
    var paragraphContent = p.textContent

    var div = createControlsContainer()

    if (paragraphContent.length != 0) {
      var analyzeButton = createButton("分析")
      div.onclick = function() {
        var textContent = parsePtoPlainText(p)
        hostAppParseParagraph(textContent.substring(0, textContent.length - 2))
      };
      div.append(analyzeButton)
      p.append(div)
    }
  });
}

function isRubyContains(nodes) {
  return Array.from(nodes).find(element => element.nodeName === "ruby") != undefined;
}

function parsePtoPlainText(p) {
  var plainText = ""
  var nodes = p.childNodes
    
  nodes.forEach(node => {
    if (node.nodeName == "ruby") {
      node.childNodes.forEach(n => {
        if (n.nodeName == "rt") {
          // skip this tag
        } else {
          plainText += n.textContent
        }
      })
    } else if (node.childNodes.length > 0 && isRubyContains(node.childNodes)) {
        plainText += parsePtoPlainText(node)
    } else {
      plainText += node.textContent
    }
  });

  return plainText
}

function addStyle (styleText) {
  const styleNode = document.createElement('style');
  styleNode.type = 'text/css';
  styleNode.textContent = styleText;
  document.documentElement.appendChild(styleNode);
  return styleNode;
}

window.onload= () => {
  var style = `
  div.pd-controls {
    display: inline-block !important;
    text-indent: 0 !important;
    margin: 0 !important;
    padding: 0 !important;
    line-height: 1.0em !important;
    -webkit-tap-highlight-color: transparent;
    -webkit-touch-callout: none; /* iOS Safari */
    -webkit-user-select: none; /* Safari */
    vertical-align: 10% !important;
  }

  div.pd-button {
    font-size: 0.8em !important;
    font-weight: bold !important;
    font-family: sans-serif !important;
    text-indent: 0 !important;
    margin: 0 !important;
    display: inline-block !important;
    border: 1px solid #31BB56 !important;
    border-radius: 200px !important;
    padding: 0.35em 0.75em !important;
    line-height: 1.0em !important;
    vertical-align: middle !important;
  }

  :root[style*="readium-font-on"][style*="--USER__fontFamily"] div.pd-button {
    font-family: sans-serif !important;
  }

  * {
    -moz-writing-mode: horizontal-tb !important;
    -ms-writing-mode: horizontal-tb !important;
    -o-writing-mode: horizontal-tb !important;
    -webkit-writing-mode: horizontal-tb !important;
    writing-mode: horizontal-tb !important;
  }
  `
  addStyle(style)
  injectParagraph()
}

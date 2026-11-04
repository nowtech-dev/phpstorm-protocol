// revision: 7
var url = WScript.Arguments(0);

function fail(message) {
  var shell = new ActiveXObject('WScript.Shell');
  shell.Popup(message, 0, 'PhpStorm Protocol', 16);
  WScript.Quit(1);
}

function readUtf8File(path) {
  var stream = new ActiveXObject('ADODB.Stream');
  stream.Type = 2;
  stream.Charset = 'utf-8';
  stream.Open();
  stream.LoadFromFile(path);
  var text = stream.ReadText();
  stream.Close();
  return text;
}

function includeFile(filename) {
  var fsoLocal = new ActiveXObject('Scripting.FileSystemObject');
  var full = WScript.ScriptFullName;
  var currentDirectory = full.substring(0, full.lastIndexOf('\\'));
  var fileStream = fsoLocal.openTextFile(currentDirectory + '\\' + filename);
  var fileData = fileStream.readAll();
  fileStream.Close();
  eval(fileData);
}

function normalizeIncomingUrl(raw) {
  var url = raw;
  if (!url) {
    return '';
  }
  url = String(url).replace(/^\s+|\s+$/g, '');
  while (url.length > 0 && (url.charAt(0) === '\\' || url.charAt(0) === '/')) {
    url = url.substring(1);
  }
  while (url.length > 0 && (url.charAt(url.length - 1) === '\\' || url.charAt(url.length - 1) === '/')) {
    url = url.substring(0, url.length - 1);
  }
  if (url.length > 1 && url.charAt(0) === '"' && url.charAt(url.length - 1) === '"') {
    url = url.substring(1, url.length - 1);
  }
  return url;
}

function parseProtocolUrl(rawUrl) {
  var url = normalizeIncomingUrl(rawUrl);
  try {
    url = decodeURIComponent(url);
  } catch (e) {
    url = normalizeIncomingUrl(rawUrl);
  }

  var match =
    /^(?:phpstorm|idea):\/\/open\/?\?(?:url=file:\/\/|file=)(.+?)(?:&line=(\d+))?$/i.exec(url);
  if (!match) {
    return null;
  }

  var file = decodeURIComponent(match[1].replace(/\+/g, ' '));
  file = file.replace(/^[\\\/]+([A-Za-z]:)/, '$1');
  if (file.indexOf('file://') === 0) {
    file = file.substring(7);
  }
  file = file.replace(/^[\\\/]+([A-Za-z]:)/, '$1');

  var result = {};
  result.file = file;
  result.line = match[2] || '';
  return result;
}

includeFile('json2.js');

var parsed = parseProtocolUrl(url);
if (!parsed) {
  fail('Unsupported URL:\n' + url);
}

var file = parsed.file;
var line = parsed.line;

var shell = new ActiveXObject('WScript.Shell');
var fso = new ActiveXObject('Scripting.FileSystemObject');
var statePath =
  shell.ExpandEnvironmentStrings('%USERPROFILE%') + '\\.jetbrains\\phpstorm-protocol\\state.json';

if (!fso.FileExists(statePath)) {
  fail(
    'Protocol state not found.\nOpen a project in PhpStorm with the PhpStorm Protocol plugin, then try again.\n' +
      statePath
  );
}

var state = JSON.parse(readUtf8File(statePath));
var launcher = state.defaultLauncher;
var project = '';

function normalizePathKey(path) {
  if (!path) {
    return '';
  }
  path = path.replace(/[\\\/]+$/, '');
  try {
    return fso.GetAbsolutePathName(path).replace(/[\\\/]+$/, '');
  } catch (e) {
    return path.toLowerCase();
  }
}

var filePath = file.replace(/\//g, '\\');
var isFolder = fso.FolderExists(filePath);
var isFile = fso.FileExists(filePath);

if (!isFolder && !isFile) {
  fail('Path not found:\n' + filePath + '\n\nFull URL:\n' + url);
}

if (isFolder) {
  project = fso.GetFolder(filePath).Path;
} else {
  var dir = fso.GetFile(filePath).ParentFolder;
  while (dir) {
    if (fso.FolderExists(dir.Path + '\\.idea')) {
      project = dir.Path;
      break;
    }
    dir = dir.ParentFolder;
  }
}

if (project && state.projects) {
  var projectKey = normalizePathKey(project);
  for (var key in state.projects) {
    if (!state.projects.hasOwnProperty(key)) {
      continue;
    }
    if (normalizePathKey(key) === projectKey) {
      var entry = state.projects[key];
      if (entry.launcher && fso.FileExists(entry.launcher)) {
        launcher = entry.launcher;
      }
      break;
    }
  }
}

if (!launcher || !fso.FileExists(launcher)) {
  fail('PhpStorm launcher not found.\n' + launcher);
}

var cmd = '"' + launcher + '"';
if (project) {
  cmd += ' "' + project + '"';
}
if (line) {
  cmd += ' --line ' + line;
}
if (!isFolder) {
  cmd += ' "' + filePath + '"';
}

shell.Exec(cmd);

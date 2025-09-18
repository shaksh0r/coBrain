import JSZip from 'jszip';
import { requestDocumentState } from '../../API/crdtwebsocket';
import { SiJavascript, SiReact, SiPython, SiTypescript, SiHtml5, SiCss3,
    SiJson, SiMarkdown, SiPhp, SiGo, SiRuby, SiSwift, SiKotlin, SiRust, SiScala,
    SiShell, SiYaml, SiXml, SiDocker, SiDart, SiPerl, SiR,
    SiLatex, SiCoffeescript, SiVuedotjs, SiSvelte, SiAstro, SiC, SiCplusplus,
    SiLess, SiScikitlearn, SiMake } from 'react-icons/si';
import { FaDatabase, FaFileAlt, FaJava } from 'react-icons/fa';

export const getFileIcon = (fileName) => {
    if (!fileName || typeof fileName !== 'string') return <FaFileAlt />;
    const ext = fileName.split('.').pop().toLowerCase();
    switch (ext) {
        case 'java':
            return <FaJava color="#e76f00" />;
        case 'js':
            return <SiJavascript color="#f7e018" />;
        case 'jsx':
            return <SiReact color="#61dafb" />;
        case 'ts':
        case 'tsx':
            return <SiTypescript color="#3178c6" />;
        case 'py':
        case 'pyw':
            return <SiPython color="#3776ab" />;
        case 'cpp':
        case 'cxx':
        case 'cc':
        case 'hpp':
        case 'h':
            return <SiCplusplus color="#00599c" />;
        case 'c':
            return <SiC color="#555555" />;
        case 'cs':
            return <FaFileAlt />;
        case 'go':
            return <SiGo color="#00add8" />;
        case 'rb':
            return <SiRuby color="#cc342d" />;
        case 'php':
            return <SiPhp color="#777bb4" />;
        case 'swift':
            return <SiSwift color="#f05138" />;
        case 'kt':
        case 'kts':
            return <SiKotlin color="#7f52ff" />;
        case 'rs':
            return <SiRust color="#dea584" />;
        case 'scala':
            return <SiScala color="#dc322f" />;
        case 'sh':
        case 'bash':
        case 'zsh':
            return <SiShell color="#89e051" />;
        case 'html':
            return <SiHtml5 color="#e34c26" />;
        case 'css':
            return <SiCss3 color="#1572b6" />;
        case 'scss':
            return <SiScikitlearn color="#f7931e" />;
        case 'less':
            return <SiLess color="#1d365d" />;
        case 'json':
            return <SiJson color="#cbcb41" />;
        case 'yaml':
        case 'yml':
            return <SiYaml color="#cb171e" />;
        case 'xml':
        case 'plist':
            return <SiXml color="#0060ac" />;
        case 'md':
        case 'markdown':
            return <SiMarkdown color="#4a4a4a" />;
        case 'txt':
        case 'log':
            return <FaFileAlt />;
        case 'dockerfile':
            return <SiDocker color="#2496ed" />;
        case 'makefile':
            return <SiMake color="#000000" />;
        case 'ini':
        case 'conf':
        case 'toml':
            return <FaFileAlt />;
        case 'bat':
            return <FaFileAlt />;
        case 'ps1':
            return <FaFileAlt />;
        case 'sql':
            return <FaDatabase color="#e86083" />;
        case 'vue':
            return <SiVuedotjs color="#42b883" />;
        case 'svelte':
            return <SiSvelte color="#ff3e00" />;
        case 'astro':
            return <SiAstro color="#ff5d01" />;
        case 'dart':
            return <SiDart color="#0175c2" />;
        case 'groovy':
            return <FaFileAlt />;
        case 'perl':
        case 'pl':
            return <SiPerl color="#39457e" />;
        case 'r':
            return <SiR color="#276dc3" />;
        case 'tex':
        case 'latex':
            return <SiLatex color="#008080" />;
        case 'coffee':
            return <SiCoffeescript color="#6f4e37" />;
        case 'asm':
            return <FaFileAlt />;
        case 'sol':
            return <FaFileAlt />;
        default:
            return <FaFileAlt />;
    }
};

export const languageMap = {
    java: 'java',
    js: 'javascript',
    jsx: 'javascript',
    ts: 'typescript',
    tsx: 'typescript',
    py: 'python',
    pyw: 'python',
    cpp: 'cpp',
    cxx: 'cpp',
    cc: 'cpp',
    c: 'c',
    h: 'cpp',
    hpp: 'cpp',
    cs: 'csharp',
    go: 'go',
    rb: 'ruby',
    php: 'php',
    swift: 'swift',
    kt: 'kotlin',
    kts: 'kotlin',
    rs: 'rust',
    scala: 'scala',
    sh: 'shell',
    bash: 'shell',
    zsh: 'shell',
    html: 'html',
    css: 'css',
    scss: 'scss',
    less: 'less',
    json: 'json',
    yaml: 'yaml',
    yml: 'yaml',
    xml: 'xml',
    md: 'markdown',
    markdown: 'markdown',
    txt: 'plaintext',
    dockerfile: 'dockerfile',
    makefile: 'makefile',
    ini: 'ini',
    conf: 'ini',
    toml: 'toml',
    bat: 'bat',
    ps1: 'powershell',
    sql: 'sql',
    plist: 'xml',
    vue: 'vue',
    svelte: 'svelte',
    astro: 'astro',
    dart: 'dart',
    groovy: 'groovy',
    perl: 'perl',
    pl: 'perl',
    r: 'r',
    tex: 'latex',
    latex: 'latex',
    coffee: 'coffeescript',
    asm: 'asm',
    sol: 'solidity',
    log: 'plaintext',
};

export const zipDirectoryContent = async (explorerFiles, sessionID, clientIdRef, zipType = 'blob') => {
    if (!explorerFiles || explorerFiles.length === 0) return null;
    const zip = new JSZip();
    for (const file of explorerFiles) {
        const content = await requestDocumentState(sessionID, file.fileID, clientIdRef);
        zip.file(file.fileName, content);
    }
    return await zip.generateAsync({ type: zipType });
};
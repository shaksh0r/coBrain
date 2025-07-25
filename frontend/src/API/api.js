import axios from 'axios';

const API = axios.create({
    baseURL: 'http://localhost:8080/',
});

export const executeCode = async (language, sourceCode) => {
    const response = await API.post("/execute", {
        "language": language,
        version: "3.14",
        "files": [
            {
                "name": "shakshor_gay.py",
                "content": sourceCode
            },
        ],
    });
    return response.data;
};